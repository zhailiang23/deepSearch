#!/usr/bin/env python3
"""
敏感词数据迁移脚本
从 PostgreSQL 迁移 sensitive_words 表数据到 MySQL

使用方法:
    uv run python scripts/migrate_sensitive_words.py
"""

import sys
from datetime import datetime
import psycopg2
import pymysql
from psycopg2.extras import RealDictCursor

# PostgreSQL 连接配置（来自 docker-compose-postgre.yml）
PG_CONFIG = {
    'host': 'localhost',
    'port': 5432,
    'database': 'mgmt_db',
    'user': 'mgmt_user',
    'password': 'mgmt_password'
}

# MySQL 连接配置（来自 docker-compose.yml）
MYSQL_CONFIG = {
    'host': 'localhost',
    'port': 3306,
    'database': 'mgmt_db',
    'user': 'mgmt_user',
    'password': 'mgmt_password',
    'charset': 'utf8mb4'
}


def connect_postgresql():
    """连接到 PostgreSQL 数据库"""
    try:
        conn = psycopg2.connect(**PG_CONFIG, cursor_factory=RealDictCursor)
        print(f"✓ 成功连接到 PostgreSQL: {PG_CONFIG['host']}:{PG_CONFIG['port']}/{PG_CONFIG['database']}")
        return conn
    except Exception as e:
        print(f"✗ 连接 PostgreSQL 失败: {e}")
        sys.exit(1)


def connect_mysql():
    """连接到 MySQL 数据库"""
    try:
        conn = pymysql.connect(**MYSQL_CONFIG)
        print(f"✓ 成功连接到 MySQL: {MYSQL_CONFIG['host']}:{MYSQL_CONFIG['port']}/{MYSQL_CONFIG['database']}")
        return conn
    except Exception as e:
        print(f"✗ 连接 MySQL 失败: {e}")
        sys.exit(1)


def fetch_sensitive_words_from_pg(pg_conn):
    """从 PostgreSQL 查询敏感词数据"""
    try:
        cursor = pg_conn.cursor()
        query = """
            SELECT id, name, harm_level, enabled,
                   created_at, updated_at, created_by, updated_by, version
            FROM sensitive_words
            ORDER BY id
        """
        cursor.execute(query)
        rows = cursor.fetchall()
        cursor.close()

        print(f"✓ 从 PostgreSQL 读取到 {len(rows)} 条敏感词记录")
        return rows
    except Exception as e:
        print(f"✗ 查询 PostgreSQL 数据失败: {e}")
        sys.exit(1)


def insert_sensitive_words_to_mysql(mysql_conn, rows):
    """将敏感词数据插入到 MySQL"""
    if not rows:
        print("⚠ 没有数据需要迁移")
        return

    try:
        cursor = mysql_conn.cursor()

        # 清空 MySQL 目标表（可选，根据需求决定）
        print("⚠ 清空 MySQL 目标表...")
        cursor.execute("DELETE FROM sensitive_words")
        mysql_conn.commit()
        print(f"✓ 已清空目标表")

        # 批量插入数据
        insert_sql = """
            INSERT INTO sensitive_words
            (id, name, harm_level, enabled, created_at, updated_at, created_by, updated_by, version)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
        """

        inserted_count = 0
        failed_count = 0

        for row in rows:
            try:
                cursor.execute(insert_sql, (
                    row['id'],
                    row['name'],
                    row['harm_level'],
                    row['enabled'],
                    row.get('created_at'),
                    row.get('updated_at'),
                    row.get('created_by'),
                    row.get('updated_by'),
                    row.get('version', 0)
                ))
                inserted_count += 1
            except pymysql.IntegrityError as e:
                # 处理重复键等完整性约束错误
                failed_count += 1
                print(f"  ⚠ 跳过重复记录: id={row['id']}, name={row['name']}")
            except Exception as e:
                failed_count += 1
                print(f"  ✗ 插入失败: id={row['id']}, error={e}")

        mysql_conn.commit()
        cursor.close()

        print(f"✓ 成功插入 {inserted_count} 条记录")
        if failed_count > 0:
            print(f"⚠ 失败/跳过 {failed_count} 条记录")

    except Exception as e:
        mysql_conn.rollback()
        print(f"✗ 插入 MySQL 数据失败: {e}")
        sys.exit(1)


def verify_migration(mysql_conn):
    """验证迁移结果"""
    try:
        cursor = mysql_conn.cursor()
        cursor.execute("SELECT COUNT(*) as count FROM sensitive_words")
        result = cursor.fetchone()
        count = result[0]
        cursor.close()

        print(f"✓ 验证完成: MySQL 中共有 {count} 条敏感词记录")
        return count
    except Exception as e:
        print(f"✗ 验证失败: {e}")
        return 0


def main():
    """主函数"""
    print("=" * 60)
    print("敏感词数据迁移工具")
    print(f"PostgreSQL → MySQL")
    print("=" * 60)
    print()

    # 连接数据库
    pg_conn = connect_postgresql()
    mysql_conn = connect_mysql()

    try:
        # 从 PostgreSQL 读取数据
        print("\n[1/3] 读取源数据...")
        rows = fetch_sensitive_words_from_pg(pg_conn)

        # 插入到 MySQL
        print("\n[2/3] 写入目标数据库...")
        insert_sensitive_words_to_mysql(mysql_conn, rows)

        # 验证迁移结果
        print("\n[3/3] 验证迁移结果...")
        count = verify_migration(mysql_conn)

        print("\n" + "=" * 60)
        print(f"✓ 数据迁移完成！共迁移 {count} 条记录")
        print("=" * 60)

    finally:
        # 关闭连接
        if pg_conn:
            pg_conn.close()
            print("\n✓ 已关闭 PostgreSQL 连接")
        if mysql_conn:
            mysql_conn.close()
            print("✓ 已关闭 MySQL 连接")


if __name__ == "__main__":
    main()
