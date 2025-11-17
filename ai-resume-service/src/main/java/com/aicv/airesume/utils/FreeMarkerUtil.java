package com.aicv.airesume.utils;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.cache.StringTemplateLoader;

import java.io.StringWriter;
import java.util.Map;

public class FreeMarkerUtil {

    private static final Configuration cfg;

    static {
        cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setTemplateLoader(new StringTemplateLoader());
        cfg.setDefaultEncoding("UTF-8");
    }

    public static String parse(String templateString, Map<String, Object> data) {
        try {
            String templateName = "dynamicTemplate_" + System.currentTimeMillis();
            ((StringTemplateLoader) cfg.getTemplateLoader())
                    .putTemplate(templateName, templateString);

            Template template = cfg.getTemplate(templateName);
            StringWriter writer = new StringWriter();
            template.process(data, writer);

            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException("FreeMarker parse error", e);
        }
    }
}

