import pandas as pd

def load_text_from_file(file):
    if file.name.endswith('.xlsx'):
        df = pd.read_excel(file)
    elif file.name.endswith('.csv'):
        df = pd.read_csv(file)
    else:
        raise ValueError("不支持的文件格式")
    
    if 'text' not in df.columns:
        raise ValueError("文件中必须包含 'text' 列")
    
    df['text'] = df['text'].astype(str).str.strip()
    df = df[df['text'].str.len() > 1]
    return df
