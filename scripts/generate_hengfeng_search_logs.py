#!/usr/bin/env python3
"""
生成恒丰银行APP搜索记录
涉及功能、产品、活动、资讯等方面的查询
"""

import random
import pymysql
from datetime import datetime, timedelta
import uuid

# 数据库连接配置
DB_CONFIG = {
    'host': 'localhost',
    'port': 3306,
    'user': 'mgmt_user',
    'password': 'mgmt_password',
    'database': 'mgmt_db',
    'charset': 'utf8mb4'
}

# 搜索空间映射 (id, code, name)
SEARCH_SPACES = [
    (7, 'function', '功能'),
    (8, 'activity', '活动'),
    (9, 'product', '产品'),
    (10, 'information', '资讯')
]

# 恒丰银行APP搜索关键词库
SEARCH_QUERIES = {
    'function': [
        '转账', '汇款', '查询余额', '账户管理', '信用卡还款',
        '手机银行登录', '指纹支付', '刷脸支付', '扫码支付', '二维码收款',
        '定期存款', '活期存款', '理财购买', '基金定投', '贷款申请',
        '密码修改', '绑定银行卡', '解绑银行卡', '账单查询', '交易明细',
        '话费充值', '生活缴费', '水电煤缴费', '信用卡办理', '开通网银',
        '外汇兑换', '跨境汇款', '预约取号', '网点查询', '客服咨询',
        '手势密码', '安全中心', '消息通知', '个人信息', '实名认证',
        '电子回单', '转账限额', '修改手机号', '开通短信提醒', '账户挂失',
        '换卡申请', '积分查询', '优惠券', '邀请好友', '签到领奖',
        '语音转账', '声纹识别', '人脸识别', 'NFC支付', '云闪付',
        '小额免密', '安全锁', '交易密码', '消费分期', '账单分期'
    ],
    'activity': [
        '新客礼遇', '开卡有礼', '推荐好友', '签到抽奖', '积分兑换',
        '理财节', '消费返现', '刷卡优惠', '商户优惠', '生日特权',
        '节日活动', '春节活动', '国庆活动', '双十一', '双十二',
        '周年庆', '感恩回馈', '邀请有礼', '分享赢奖', '抽奖活动',
        '满减活动', '折扣优惠', '限时秒杀', '新人专享', '老客回馈',
        '积分翻倍', '免费体验', '试用活动', '推广活动', '营销活动',
        '信用卡优惠', '贷款优惠', '理财优惠', '存款优惠', '汇款优惠'
    ],
    'product': [
        '恒丰宝', '恒利金', '智慧存', '定活宝', '天天利',
        '月月盈', '季季升', '年年富', '步步高', '稳健理财',
        '恒丰信用卡', '金卡', '白金卡', '钻石卡', '商务卡',
        '个人贷款', '消费贷', '经营贷', '房屋抵押贷', '车辆抵押贷',
        '恒e融', '恒速贷', '薪金贷', '公积金贷', '信用贷',
        '基金产品', '货币基金', '债券基金', '股票基金', '混合基金',
        '保险产品', '意外险', '健康险', '寿险', '财产险',
        '黄金产品', '纸黄金', '实物黄金', '黄金定投', '积存金',
        '外汇产品', '外汇买卖', '外汇理财', '结售汇', '跨境汇款',
        '企业产品', '对公账户', '企业理财', '供应链金融', '票据业务',
        '财富管理', '资产配置', '私人银行', '高端理财', 'VIP服务'
    ],
    'information': [
        '利率调整', '汇率查询', '理财收益', '产品公告', '系统升级',
        '业务通知', '风险提示', '政策解读', '金融知识', '理财攻略',
        '安全提示', '防诈骗', '新功能上线', '版本更新', '服务时间',
        '网点信息', '营业时间', '联系方式', '投诉建议', '帮助中心',
        '操作指南', '常见问题', '新手教程', '视频教程', '图文教程',
        '行业动态', '市场分析', '经济新闻', '股市行情', '基金净值',
        '监管政策', '合规要求', '隐私政策', '用户协议', '服务条款',
        '优惠资讯', '活动公告', '中奖名单', '积分规则', '会员权益',
        '产品对比', '收益计算', '费用说明', '额度查询', '办理流程'
    ]
}

# User Agents
USER_AGENTS = [
    'HengfengBank/5.2.1 (iPhone; iOS 16.5.1; Scale/3.00)',
    'HengfengBank/5.2.1 (iPhone; iOS 17.0; Scale/3.00)',
    'HengfengBank/5.2.0 (Android 13; Xiaomi MI 12)',
    'HengfengBank/5.2.0 (Android 12; HUAWEI P50)',
    'HengfengBank/5.2.1 (Android 14; Samsung Galaxy S23)',
    'HengfengBank/5.1.8 (iPhone; iOS 15.7; Scale/2.00)',
    'HengfengBank/5.2.0 (Android 11; OPPO Find X5)',
    'HengfengBank/5.2.1 (Android 13; vivo X90)',
]

# IP地址段
IP_PREFIXES = [
    '192.168.1', '192.168.2', '10.0.0', '10.1.0',
    '172.16.1', '172.16.2', '183.14.', '114.80.'
]

def generate_ip():
    """生成随机IP地址"""
    prefix = random.choice(IP_PREFIXES)
    if '.' in prefix[-1]:
        return f"{prefix}{random.randint(1, 254)}.{random.randint(1, 254)}"
    return f"{prefix}.{random.randint(1, 254)}"

def generate_trace_id():
    """生成追踪ID"""
    return str(uuid.uuid4()).replace('-', '')

def generate_session_id():
    """生成会话ID"""
    return str(uuid.uuid4()).replace('-', '')

def random_datetime(start_days_ago=30):
    """生成随机时间（最近N天内）"""
    now = datetime.now()
    start = now - timedelta(days=start_days_ago)
    random_second = random.randint(0, int((now - start).total_seconds()))
    return start + timedelta(seconds=random_second)

def generate_search_log():
    """生成一条搜索日志记录"""
    # 随机选择搜索空间
    space_id, space_code, space_name = random.choice(SEARCH_SPACES)

    # 随机选择搜索关键词
    search_query = random.choice(SEARCH_QUERIES[space_code])

    # 生成时间
    created_at = random_datetime(30)

    # 生成用户ID (假设有1-100个用户)
    user_id = random.randint(1, 100)

    # 生成性能数据
    es_time = random.randint(10, 500)
    total_time = es_time + random.randint(5, 100)
    total_results = random.randint(0, 1000)
    returned_results = min(10, total_results) if total_results > 0 else 0
    max_score = round(random.uniform(0.1, 10.0), 6) if total_results > 0 else None

    # 99%成功率
    status = 'SUCCESS' if random.random() < 0.99 else random.choice(['ERROR', 'TIMEOUT'])

    return {
        'trace_id': generate_trace_id(),
        'user_id': user_id,
        'session_id': generate_session_id(),
        'ip_address': generate_ip(),
        'user_agent': random.choice(USER_AGENTS),
        'search_space_id': space_id,
        'search_space_code': space_code,
        'search_space_name': space_name,
        'search_query': search_query,
        'search_mode': random.choice(['STANDARD', 'FUZZY', 'EXACT']),
        'enable_pinyin': random.choice([0, 1]),
        'page_number': 1,
        'page_size': 10,
        'total_results': total_results,
        'returned_results': returned_results,
        'max_score': max_score,
        'elasticsearch_time_ms': es_time,
        'total_time_ms': total_time,
        'memory_used_mb': random.randint(50, 200),
        'status': status,
        'created_at': created_at,
        'created_by': 'system',
        'updated_at': created_at,
        'updated_by': 'system'
    }

def insert_logs(logs):
    """批量插入日志"""
    conn = pymysql.connect(**DB_CONFIG)
    try:
        with conn.cursor() as cursor:
            sql = """
            INSERT INTO search_logs (
                trace_id, user_id, session_id, ip_address, user_agent,
                search_space_id, search_space_code, search_space_name,
                search_query, search_mode, enable_pinyin,
                page_number, page_size, total_results, returned_results, max_score,
                elasticsearch_time_ms, total_time_ms, memory_used_mb,
                status, created_at, created_by, updated_at, updated_by
            ) VALUES (
                %(trace_id)s, %(user_id)s, %(session_id)s, %(ip_address)s, %(user_agent)s,
                %(search_space_id)s, %(search_space_code)s, %(search_space_name)s,
                %(search_query)s, %(search_mode)s, %(enable_pinyin)s,
                %(page_number)s, %(page_size)s, %(total_results)s, %(returned_results)s, %(max_score)s,
                %(elasticsearch_time_ms)s, %(total_time_ms)s, %(memory_used_mb)s,
                %(status)s, %(created_at)s, %(created_by)s, %(updated_at)s, %(updated_by)s
            )
            """
            cursor.executemany(sql, logs)
            conn.commit()
            return cursor.rowcount
    finally:
        conn.close()

def main():
    """主函数"""
    print("开始生成恒丰银行APP搜索记录...")

    # 生成1000条记录
    batch_size = 100
    total_count = 1000
    total_inserted = 0

    for i in range(0, total_count, batch_size):
        batch_logs = [generate_search_log() for _ in range(min(batch_size, total_count - i))]
        inserted = insert_logs(batch_logs)
        total_inserted += inserted
        print(f"已插入 {total_inserted}/{total_count} 条记录")

    print(f"✅ 成功生成并插入 {total_inserted} 条恒丰银行APP搜索记录")

    # 统计信息
    conn = pymysql.connect(**DB_CONFIG)
    try:
        with conn.cursor() as cursor:
            # 按搜索空间统计
            cursor.execute("""
                SELECT search_space_name, COUNT(*) as count
                FROM search_logs
                WHERE search_space_code IN ('function', 'activity', 'product', 'information')
                GROUP BY search_space_name
                ORDER BY count DESC
            """)
            print("\n📊 各搜索空间记录数:")
            for row in cursor.fetchall():
                print(f"  {row[0]}: {row[1]} 条")

            # 热门搜索
            cursor.execute("""
                SELECT search_query, COUNT(*) as count
                FROM search_logs
                WHERE search_space_code IN ('function', 'activity', 'product', 'information')
                GROUP BY search_query
                ORDER BY count DESC
                LIMIT 10
            """)
            print("\n🔥 热门搜索TOP10:")
            for idx, row in enumerate(cursor.fetchall(), 1):
                print(f"  {idx}. {row[0]}: {row[1]} 次")
    finally:
        conn.close()

if __name__ == '__main__':
    main()
