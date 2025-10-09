# clustering.py
import umap
from sklearn.cluster import DBSCAN
import numpy as np

def perform_clustering(embeddings, texts, eps=0.35, min_samples=3, metric='cosine'):
    """
    执行 DBSCAN 聚类 + UMAP 降维
    参数：
        embeddings: 句向量列表
        texts: 原始文本列表
        eps: DBSCAN 的邻域半径
        min_samples: 核心点最小样本数
        metric: 距离度量方式 ('cosine', 'euclidean')
    返回：
        labels: 聚类标签数组 (numpy array)
        embedding_2d: UMAP 降维后的 2D 坐标 (numpy array, shape: (n_samples, 2))
    """
    # 确保输入是 numpy 数组
    embeddings = np.array(embeddings)

    # DBSCAN 聚类
    clustering = DBSCAN(eps=eps, min_samples=min_samples, metric=metric)
    labels = clustering.fit_predict(embeddings)

    # UMAP 降维（使用相同 metric）
    reducer = umap.UMAP(n_components=2, metric=metric, random_state=42)
    embedding_2d = reducer.fit_transform(embeddings)

    return labels, embedding_2d


def create_umap_plot(embedding_2d, labels, texts):
    """
    创建 UMAP 可视化图（支持 hover 文本）
    """
    embedding_2d = np.array(embedding_2d)
    labels = np.array(labels)
    texts = np.array(texts)  # 转为 numpy 支持布尔索引

    unique_labels = np.unique(labels)
    
    # 固定颜色表
    colors = [
        '#4e79a7', '#f28e2c', '#e15759', '#76b7b2', '#59a14f',
        '#edc949', '#af7aa1', '#ff9da7', '#9c755f', '#bab0ac'
    ]

    fig = go.Figure()

    for i, label in enumerate(unique_labels):
        if label == -1:
            color = 'gray'
            name = '噪声点'
            opacity = 0.3
        else:
            color = colors[i % len(colors)]
            name = f'簇 {label}'
            opacity = 0.8
        
        mask = (labels == label)
        
        # 截取文本前30字
        hover_texts = [
            t[:30] + '...' if len(str(t)) > 30 else str(t)
            for t in texts[mask]
        ]
        
        fig.add_trace(go.Scatter(
            x=embedding_2d[mask, 0],
            y=embedding_2d[mask, 1],
            mode='markers',
            marker=dict(size=8, color=color, opacity=opacity),
            name=name,
            text=hover_texts,
            hovertemplate="<b>%{text}</b><br>x: %{x:.2f}<br>y: %{y:.2f}<extra></extra>"
        ))
    
    fig.update_layout(
        title="用户反馈语义聚类分布（UMAP）",
        xaxis_title="UMAP 1",
        yaxis_title="UMAP 2",
        height=600,
        showlegend=True,
        legend=dict(title="聚类结果", orientation="v", yanchor="top", y=0.99, x=0.01)
    )
    
    return fig