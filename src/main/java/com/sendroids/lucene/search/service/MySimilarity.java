package com.sendroids.lucene.search.service;

import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.util.BytesRef;


/**
 * q -> query, d -> document, t -> term
 *
 * score(q,d)   =   coord-factor(q,d) ·   query-boost(q) ·   V(q) · V(d) / |V(q)|    ·   doc-len-norm(d)   ·   doc-boost(d)
 *
 * score(q,d)   =   coord(q,d)  ·  queryNorm(q)  · 	 ∑(t in q)	( tf(t in d)  ·  idf(t)^2  ·  t.getBoost() ·  norm(t,d) )
 *
 */
public class MySimilarity extends BM25Similarity {
    /**
     * coord(q,d) - 文档覆盖的搜索词
     *
     * 意义： 对于查询中的词，文档中包含这些词越多分值越高
     * 说明： 分值高的文档要更多地覆盖查询中的词
     *
     * @param overlap    the number of query terms matched in the document
     * @param maxOverlap the total number of terms in the query
     * @return a score factor based on term overlap with the query
     */
    @Override
    public float coord(int overlap, int maxOverlap) {
//        return (float) overlap / (float) maxOverlap;
        return (float) overlap / (float) maxOverlap * 2;
    }

    /**
     * queryNorm(q) - 不影响评分，search时
     *
     * 意义： 和文档的相关性无关，只是为了让不同的查询之间的分值有可比性
     *
     * @param sumOfSquaredWeights the sum of the squares of query term weights
     * @return a normalization factor for query weights
     */
    @Override
    public float queryNorm(float sumOfSquaredWeights) {
        return (float) (1.0D / Math.sqrt((double) sumOfSquaredWeights));
    }

    /**
     * 词频-文档内
     *
     * 意义： 一个词在文档里出现的频率越高，则该文档的分值越高
     * 说明： 多次包含同一个词的文档的相关度更高
     *
     * @param freq the frequency of a term within a document
     * @return a score factor based on a term's within-document frequency
     */
    public float tf(float freq) {
//            tf(t in d)   =  	frequency½
        return (float) Math.sqrt((double) freq) * 2;
    }

    /**
     * 词频-所有文档
     *
     * 意义： 一个词越多出现在不同的文档里，则它的分值越低
     * 说明： 常见词的重要性要低于不常见的词
     *
     * @param docFreq  the number of documents which contain the term
     * @param docCount the total number of documents in the collection
     * @return a score factor based on the term's document frequency
     */
    @Override
    public float idf(long docFreq, long docCount) {
        return (float)Math.log(1.0D + ((double)(docCount - docFreq) + 0.5D) / ((double)docFreq + 0.5D));
//        return (float) (Math.log((double) (docCount + 1L) / (double) (docFreq + 1L)) + 1.0D);
    }

    /**
     * 间隔
     *
     * @param distance the edit distance of this sloppy phrase match
     * @return the frequency increment for this match
     */
    @Override
    public float sloppyFreq(int distance) {
        return 1.0F / (float) (distance + 1);
    }

    /**
     * @param doc     The docId currently being scored.
     * @param start   The start position of the payload
     * @param end     The end position of the payload
     * @param payload The payload byte array to be scored
     * @return An implementation dependent float to be used as a scoring factor
     */
    @Override
    public float scorePayload(int doc, int start, int end, BytesRef payload) {
        return 1.0F;
    }

}