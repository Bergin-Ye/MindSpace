package com.mindspace.document;


/*
* 文本切块工具
* 统一换行符
* 从0开始每次切`chunkSize`个字符
* 然后下一次起点向前移动`chunkSize`个字符 保证重叠
* 超过maxchunkx就停 防止打爆我的向量费用
* */

import java.util.ArrayList;
import java.util.List;

public class TextChunker {

    private TextChunker() {}

    //定义切文本块方法 返回一个字符串列表 需要传过来需要切的文本 切块大小 重叠大小 最大切块数量
    public static List<String> split(String text, int chunkSize, int overlap, int maxChunks){
        //弄一个列表 来存储切块后的文本
        List<String> chunks = new ArrayList<>();
        //检验传过来的值是否合法
        if(text == null || text.isEmpty() || chunkSize <= 0 || maxChunks <= 0){
            //如果传过来的值不合法 直接返回空列表
            return chunks;
        }

        //步长是你切块大小减去重叠大小
        int step = chunkSize - overlap;
        if(step <= 0){
            throw new IllegalArgumentException("chunkSize 必须大于 overlap");
        }

        //统一换行符（文件有可能从linux 或者 windows 都有可能 反正统一为linux的换行符）
        String normalized = text.replace("\r\n", "\n")
                //替换windows的换行符
                .replace("\r", "\n")
                //去掉首尾空格
                .trim();
        //重新获取文本长度
        int length = normalized.length();

        //循环切块 起点要小于文本长度（最后一个范围） 并且 切块数量要小于最大切块数量
        //每次新增步长 保证重叠大小
        for (int start = 0; start < length && chunks.size() < maxChunks; start += step) {
            //算一下下一步位置 和 结束位置 不要超出最大范围
            //Math.min() -> (500(下一步结束位置)，1200（文本最大长度）)
            int end = Math.min(start + chunkSize, length);
            //去掉切块后的首位空格
            String chunk = normalized.substring(start, end).trim();
            //如果切块后不为空 就加入列表
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            //如果切块后已经到了文本的最后一个字符 就跳出循环
            if (end == length) {
                break;
            }
        }
        //返回切块后的文本列表
        return chunks;

    }

}
