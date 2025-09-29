import streamlit as st
import pandas as pd
import numpy as np
from llm_topic import generate_all_cluster_descriptions  # 导入批量生成话题的函数
import os

# ======================
# 配置API密钥（直接硬编码）
# ======================
# 注意：此方式不适合公开代码仓库，仅用于内部使用
API_KEY = "yk-4a72dfebe1a4455fb04e4e07c82c3d18"  # 你的72B模型API密钥

# ======================
# 页面配置
# ======================
st.set_page_config(page_title="💬 用户反馈聚类分析", layout="wide")
st.title("💬 用户反馈语义聚类分析系统")
st.markdown("基于 BGE 向量 + DBSCAN 聚类 + 72B大模型话题提炼")

# ======================
# 1. 数据输入
# ======================
st.header("1. 数据输入")
uploaded_file = st.file_uploader("📤 上传用户反馈 Excel 文件（单列 'text'）", type=["xlsx"])

if uploaded_file is not None:
    df = pd.read_excel(uploaded_file)
    st.success("✅ 文件上传成功！")
elif os.path.exists("data/sample.xlsx"):
    df = pd.read_excel("data/sample.xlsx")
    st.info("✅ 使用内置示例数据：`data/sample.xlsx`")
else:
    st.error("❌ 未找到 `data/sample.xlsx`，请上传文件。")
    st.stop()

# 确保数据包含'text'列
if 'text' not in df.columns:
    st.error("❌ 上传的文件必须包含'text'列")
    st.stop()

texts = df["text"].astype(str).tolist()
st.write(f"📄 共加载 **{len(texts)}** 条用户反馈")

# ======================
# 2. 聚类参数设置
# ======================
st.header("2. 聚类参数设置")

col1, col2, col3 = st.columns(3)

with col1:
    metric = st.selectbox(
        "距离度量方式",
        options=["cosine", "euclidean"],
        index=0,
        help="推荐 cosine：更适合文本语义相似性"
    )

with col2:
    eps = st.slider(
        "DBSCAN eps（簇半径）",
        min_value=0.1,
        max_value=1.0,
        value=0.4,
        step=0.05,
        help="值越大，簇越少；太小会导致碎片化（当前建议：0.4~0.5）"
    )

with col3:
    min_samples = st.slider(
        "DBSCAN min_samples",
        min_value=2,
        max_value=10,
        value=3,
        help="核心点最小邻居数，值越大噪声越多"
    )

# ======================
# 3. 开始聚类与话题分析
# ======================
if st.button("🚀 开始聚类与话题分析") or 'cluster_results' not in st.session_state:
    with st.spinner("🧠 正在生成向量并聚类..."):
        try:
            from bge_embedding import get_embeddings
            from clustering import perform_clustering

            # 获取向量
            embeddings = get_embeddings(texts)
            st.write("✅ 向量生成完成")

            # 执行聚类
            labels, fig = perform_clustering(
                embeddings=embeddings,
                texts=texts,
                eps=eps,
                min_samples=min_samples,
                metric=metric
            )

            # 保存聚类结果到DataFrame
            df_result = pd.DataFrame({
                "text": texts,
                "cluster": labels
            })

            # 保存结果到会话状态
            st.session_state.cluster_results = {
                "labels": labels,
                "fig": fig,
                "texts": texts,
                "df": df_result
            }
            st.success("✅ 聚类完成！")

        except Exception as e:
            st.error(f"❌ 聚类失败：{str(e)}")
            st.exception(e)
            st.stop()

# ======================
# 4. 聚类结果可视化
# ======================
if 'cluster_results' in st.session_state:
    results = st.session_state.cluster_results
    labels = results['labels']
    fig = results['fig']
    df_result = results['df']

    # 显示聚类可视化，添加标题为步骤3
    st.header("3. 用户反馈语义聚类分布（UMAP）")
    st.plotly_chart(fig, use_container_width=True)

    # 聚类统计
    n_clusters = len(set(labels)) - (1 if -1 in labels else 0)
    n_noise = list(labels).count(-1)
    st.write(f"📊 **聚类统计**：共 `{n_clusters}` 个有效簇，`{n_noise}` 条噪声点")

    # 生成并显示话题分析
    st.header("4. 每簇话题分析")
    with st.spinner("🧠 正在调用72B模型生成话题描述..."):
        # 直接使用硬编码的API密钥
        cluster_descriptions = generate_all_cluster_descriptions(df_result, api_key=API_KEY)

    # 显示噪声点
    noise_texts = df_result[df_result["cluster"] == -1]["text"].tolist()
    with st.expander(f"🔹 噪声点（未归类） - 共 {len(noise_texts)} 条", expanded=False):
        for t in noise_texts[:10]:
            st.markdown(f"- `{t[:80]}...`" if len(t) > 80 else f"- `{t}`")
        if len(noise_texts) > 10:
            st.markdown(f"... 还有 {len(noise_texts)-10} 条未显示")

    # 显示每个簇的话题分析
    for cluster_id in sorted(cluster_descriptions.keys()):
        desc = cluster_descriptions[cluster_id]
        cluster_size = len(df_result[df_result["cluster"] == cluster_id])
        
        with st.expander(
            f"🔹 簇 {cluster_id}：{desc['topic']}（{cluster_size}条反馈）",
            expanded=True
        ):
            st.markdown(f"**🏷️ 业务标签**：{' '.join([f'`{tag}`' for tag in desc['tags']])}")
            st.markdown("**🎯 代表性问题**：")
            for i, ex in enumerate(desc["examples"], 1):
                display_text = ex[:100] + "..." if len(ex) > 100 else ex
                st.markdown(f"{i}. `{display_text}`")

# ======================
# 提示信息
# ======================
st.markdown("---")
st.caption("💡 提示：调整参数后点击「开始聚类与话题分析」可重新运行，系统已内置72B模型API密钥。")