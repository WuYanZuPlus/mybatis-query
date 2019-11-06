package com.jianghu.winter.query.cache;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * @author daniel.hu
 */
public class CacheUtil {
    private CacheUtil() {
    }

    /**
     * 不序列化null值对应的key
     */
    public static String transformObjectToCacheKey(Object query) {
        return ToStringBuilder.reflectionToString(query, new NonNullToStringStyle());
    }

    static final class NonNullToStringStyle extends ToStringStyle {

        NonNullToStringStyle() {
            super();

            this.setUseClassName(false);
            this.setUseIdentityHashCode(false);

            this.setContentStart("{");
            this.setContentEnd("}");

            this.setArrayStart("[");
            this.setArrayEnd("]");

            this.setFieldSeparator(",");
            this.setFieldNameValueSeparator(":");

            this.setNullText("null");

            this.setSummaryObjectStartText("\"<");
            this.setSummaryObjectEndText(">\"");

            this.setSizeStartText("\"<size=");
            this.setSizeEndText(">\"");
        }

        @Override
        public void append(StringBuffer buffer, String fieldName, Object value, Boolean fullDetail) {
            if (value != null) {
                super.append(buffer, fieldName, value, fullDetail);
            }
        }

    }

}
