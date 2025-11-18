package com.aicv.airesume.utils;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.springframework.core.io.ClassPathResource;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MiniProgramPdfUtil {

    public String generatePdfFromWxml(
            String wxmlContent,
            String wxssContent,
            Map<String, Object> dataMap,
            String outputDir
    ) {
        try {
            Map<String, Object> model = prepareModel(dataMap);
            String htmlBody = convertWxmlToHtml(wxmlContent);
            String css = wxssContent.replace("rpx", "px");
            String finalHtml =
                    "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "<meta charset='UTF-8'/>\n" +
                    "<style>\n" +
                    "body { font-family: 'simhei', Arial, sans-serif; }\n" +
                    css +
                    "</style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    htmlBody +
                    "\n</body>\n" +
                    "</html>";

            String processedHtml = renderWithFreemarker(finalHtml, model);
            return generatePdf(processedHtml, outputDir);

        } catch (Exception e) {
            throw new RuntimeException("PDF 生成失败", e);
        }
    }

    private Map<String, Object> prepareModel(Map<String, Object> dataMap) {
        Map<String, Object> model = new HashMap<>();
        if (dataMap != null) {
            model.putAll(dataMap);
        }

        Object existingResumeData = dataMap == null ? null : dataMap.get("resumeData");
        if (existingResumeData instanceof Map) {
            model.put("resumeData", existingResumeData);
        } else {
            Map<String, Object> resumeData = new HashMap<>();
            if (dataMap != null) {
                resumeData.put("educationList", dataMap.get("educationList"));
                resumeData.put("workExperienceList", dataMap.get("workExperienceList"));
                resumeData.put("projectList", dataMap.get("projectList"));
                resumeData.put("skills", dataMap.get("skillList"));
                resumeData.put("skillsWithLevel", dataMap.get("skillList"));

                Map<String, Object> personalInfo = new HashMap<>();
                personalInfo.put("avatar", (dataMap.containsKey("userInfo") && ((Map) dataMap.get("userInfo")).get("avatarUrl") != null)
                        ? ((Map) dataMap.get("userInfo")).get("avatarUrl")
                        : dataMap.get("avatar"));
                personalInfo.put("jobTitle", dataMap.get("jobTitle"));
                personalInfo.put("expectedSalary", dataMap.get("expectedSalary"));
                personalInfo.put("startTime", dataMap.get("startTime"));
                personalInfo.put("selfEvaluation", dataMap.get("selfEvaluation"));
                personalInfo.put("interests", dataMap.get("interests"));
                resumeData.put("personalInfo", personalInfo);

                resumeData.put("name", dataMap.get("name"));
                resumeData.put("avatar", personalInfo.get("avatar"));
                resumeData.put("title", dataMap.get("jobTitle"));
                resumeData.put("expectedSalary", dataMap.get("expectedSalary"));
            }
            model.put("resumeData", resumeData);
        }

        if (!model.containsKey("userInfo")) {
            Object userInfo = dataMap == null ? null : dataMap.get("userInfo");
            if (userInfo instanceof Map) {
                model.put("userInfo", userInfo);
            } else {
                model.put("userInfo", new HashMap<String, Object>());
            }
        }

        return model;
    }

    private String convertWxmlToHtml(String wxml) {
        String html = wxml;

        // {{ ... }} -> ${...} 并处理 || 为 !
        Pattern mustachePattern = Pattern.compile("\\{\\{(.*?)\\}\\}", Pattern.DOTALL);
        Matcher m = mustachePattern.matcher(html);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String inner = m.group(1).trim();
            inner = inner.replaceAll("\\|\\|", "!");

            Pattern ternaryAndPattern = Pattern.compile("([\\w\\.]+)\\s*&&\\s*([\\w\\.]+)\\s*\\?\\s*([^:]+)\\s*:\\s*(.+)");
            Matcher ternaryMatcher = ternaryAndPattern.matcher(inner);
            if (ternaryMatcher.find()) {
                String v1 = ternaryMatcher.group(1);
                String v2 = ternaryMatcher.group(2);
                String thenPart = ternaryMatcher.group(3).trim();
                String elsePart = ternaryMatcher.group(4).trim();
                String replacement = "(" + v1 + "?has_content && " + v2 + "?has_content) ? " + thenPart + " : " + elsePart;
                inner = replacement;
            }
            String fmExpr = "${" + inner + "}";
            m.appendReplacement(sb, Matcher.quoteReplacement(fmExpr));
        }
        m.appendTail(sb);
        html = sb.toString();

        // wx:for -> <#list ... as item>
        Pattern forPattern = Pattern.compile("<block\\s+wx:for=\"\\$\\{(.*?)\\}\"\\s+wx:key=\"[^\"]*\">", Pattern.DOTALL);
        Matcher forMatcher = forPattern.matcher(html);
        html = forMatcher.replaceAll("<#list $1 as item>");
        html = html.replaceAll("</block>", "</#list>");

        // wx:if -> <#if ...>
        Pattern ifOpenPattern = Pattern.compile("<([a-zA-Z0-9-_]+)([^>]*)\\swx:if=\"\\$\\{(.*?)\\}\"([^>]*)>", Pattern.DOTALL);
        Matcher ifOpenMatcher = ifOpenPattern.matcher(html);
        StringBuilder htmlBuilder = new StringBuilder(html);
        int offset = 0;
        int idx = 0;
        while (ifOpenMatcher.find()) {
            String fullMatch = ifOpenMatcher.group(0);
            String tagName = ifOpenMatcher.group(1);
            String beforeAttrs = ifOpenMatcher.group(2);
            String expr = ifOpenMatcher.group(3).trim();
            String afterAttrs = ifOpenMatcher.group(4);

            String startMarker = "<<<FM_IF_START_" + (idx) + ">>>";
            String endMarker = "<<<FM_IF_END_" + (idx) + ">>>";

            int realStart = htmlBuilder.indexOf(fullMatch, offset);
            if (realStart < 0) {
                offset = 0;
                realStart = htmlBuilder.indexOf(fullMatch, offset);
                if (realStart < 0) {
                    idx++;
                    continue;
                }
            }

            String openingTagWithoutIf = "<" + tagName + beforeAttrs + afterAttrs + ">";
            htmlBuilder.replace(realStart, realStart + fullMatch.length(), startMarker + openingTagWithoutIf);

            int searchFrom = realStart + startMarker.length() + openingTagWithoutIf.length();
            String closingTag = "</" + tagName + ">";
            int closePos = htmlBuilder.indexOf(closingTag, searchFrom);
            if (closePos >= 0) {
                htmlBuilder.insert(closePos + closingTag.length(), endMarker);
                htmlBuilder.replace(realStart, realStart + startMarker.length(), "<#if " + expr + ">");
                int endMarkerPos = htmlBuilder.indexOf(endMarker, realStart + ("<#if " + expr + ">").length());
                if (endMarkerPos >= 0) {
                    htmlBuilder.replace(endMarkerPos, endMarkerPos + endMarker.length(), "</#if>");
                }
            } else {
                htmlBuilder.replace(realStart, realStart + startMarker.length(), "<#if " + expr + ">");
            }

            offset = realStart + 1;
            idx++;
            ifOpenMatcher = ifOpenPattern.matcher(htmlBuilder.toString());
        }

        html = htmlBuilder.toString();

        html = html.replaceAll("<image", "<img");
        html = html.replaceAll("<view", "<div");
        html = html.replaceAll("</view>", "</div>");
        html = html.replaceAll("<img([^>/]*?)>", "<img$1 />");

        return html;
    }

    private String renderWithFreemarker(String htmlTemplate, Map<String, Object> data) throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        freemarker.cache.StringTemplateLoader loader = new freemarker.cache.StringTemplateLoader();
        loader.putTemplate("tpl", htmlTemplate);
        cfg.setTemplateLoader(loader);
        cfg.setDefaultEncoding("UTF-8");

        cfg.setClassicCompatible(false);
        cfg.setLogTemplateExceptions(false);
        cfg.setWrapUncheckedExceptions(true);

        Template template = cfg.getTemplate("tpl");
        StringWriter sw = new StringWriter();
        template.process(data, sw);
        return sw.toString();
    }

    private String generatePdf(String html, String outputDir) throws Exception {
        File dir = new File(outputDir);
        if (!dir.exists()) dir.mkdirs();

        String fileName = UUID.randomUUID().toString() + ".pdf";
        String outputPath = outputDir + File.separator + fileName;

        try (OutputStream os = new FileOutputStream(outputPath)) {
            PdfRendererBuilder builder = new PdfRendererBuilder();

            ClassPathResource fontResource = new ClassPathResource("fonts/simhei.ttf");
            File tempFontFile = File.createTempFile("simhei", ".ttf");
            try (InputStream is = fontResource.getInputStream();
                 OutputStream fos = new FileOutputStream(tempFontFile)) {
                byte[] buffer = new byte[4096];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
            }
            tempFontFile.deleteOnExit();

            builder.useFont(tempFontFile, "simhei");
            builder.withHtmlContent(html, null);
            builder.toStream(os);
            builder.run();
        }

        return outputPath;
    }

    public static void main(String[] args) {
        MiniProgramPdfUtil util = new MiniProgramPdfUtil();
        String wxml = "<template name=\"template-one\">\r\n" + //
                        "  <view class=\"template-one-container\">\r\n" + //
                        "\r\n" + //
                        "    <!-- 左侧：教育 / 工作 / 项目 / 自我评价 -->\r\n" + //
                        "    <view class=\"left-panel-swapped\">\r\n" + //
                        "      <!-- 教育经历 -->\r\n" + //
                        "      <view class=\"section\">\r\n" + //
                        "        <view class=\"section-title\">教育经历</view>\r\n" + //
                        "        <block wx:for=\"{{resumeData.education || resumeData.educationList}}\" wx:key=\"index\">\r\n" + //
                        "          <view class=\"job\">\r\n" + //
                        "            <view class=\"job-header\">\r\n" + //
                        "              <view class=\"job-company\">{{item.school || item.schoolName || '学校名称'}}</view>\r\n" + //
                        "              <view class=\"job-position\">{{item.degree || '学位'}}</view>\r\n" + //
                        "              <view class=\"job-period\">{{item.startDate || item.startTime || ''}}{{item.startDate && item.endDate ? ' - ' : ''}}{{item.endDate || item.endTime || ''}}</view>\r\n" + //
                        "            </view>\r\n" + //
                        "            <view class=\"job-desc\">{{item.major || item.description || ''}}</view>\r\n" + //
                        "          </view>\r\n" + //
                        "        </block>\r\n" + //
                        "      </view>\r\n" + //
                        "\r\n" + //
                        "      <!-- 工作经历 -->\r\n" + //
                        "      <view class=\"section\">\r\n" + //
                        "        <view class=\"section-title\">工作经历</view>\r\n" + //
                        "        <block wx:for=\"{{resumeData.workExperience || resumeData.workExperienceList}}\" wx:key=\"index\">\r\n" + //
                        "          <view class=\"job\">\r\n" + //
                        "            <view class=\"job-header\">\r\n" + //
                        "              <view class=\"job-company\">{{item.company || item.companyName || '公司名称'}}</view>\r\n" + //
                        "              <view class=\"job-position\">{{item.position || item.jobTitle || '职位名称'}}</view>\r\n" + //
                        "              <view class=\"job-period\">{{item.startDate || item.startTime || ''}}{{item.startDate && item.endDate ? ' - ' : ''}}{{item.endDate || item.endTime || ''}}</view>\r\n" + //
                        "            </view>\r\n" + //
                        "            <view class=\"job-desc\">\r\n" + //
                        "              {{item.description || item.workContent || '工作描述'}}\r\n" + //
                        "            </view>\r\n" + //
                        "          </view>\r\n" + //
                        "        </block>\r\n" + //
                        "      </view>\r\n" + //
                        "\r\n" + //
                        "      <!-- 兴趣爱好 -->\r\n" + //
                        "      <view class=\"section\">\r\n" + //
                        "        <view class=\"section-title\">兴趣爱好</view>\r\n" + //
                        "        <view class=\"job-desc\">\r\n" + //
                        "          {{resumeData.personalInfo.interests || resumeData.selfEvaluation || '暂无兴趣爱好信息'}}\r\n" + //
                        "        </view>\r\n" + //
                        "      </view>\r\n" + //
                        "      \r\n" + //
                        "      <!-- 自我评价 -->\r\n" + //
                        "      <view class=\"section\">\r\n" + //
                        "        <view class=\"section-title\">自我评价</view>\r\n" + //
                        "        <view class=\"job-desc\">\r\n" + //
                        "          {{resumeData.personalInfo.selfEvaluation || resumeData.selfEvaluation || '无'}}\r\n" + //
                        "        </view>\r\n" + //
                        "      </view>\r\n" + //
                        "    </view>\r\n" + //
                        "\r\n" + //
                        "    <!-- 右侧：头像 + 联系方式 + 技能 -->\r\n" + //
                        "    <view class=\"right-panel-swapped\">\r\n" + //
                        "      <view class=\"avatar-container\">\r\n" + //
                        "        <image class=\"avatar\" src=\"{{resumeData.personalInfo.avatar || resumeData.avatar || '/images/avatar.jpg'}}\" mode=\"cover\" />\r\n" + //
                        "        <view class=\"name\">{{userInfo.name || resumeData.name || '姓名'}}</view>\r\n" + //
                        "        <view class=\"title\">{{resumeData.personalInfo.jobTitle || resumeData.personalInfo.position || resumeData.title || '职位'}}</view>\r\n" + //
                        "      </view>\r\n" + //
                        "\r\n" + //
                        "      <!-- 联系方式 -->\r\n" + //
                        "      <view class=\"contact-section\">\r\n" + //
                        "        <view class=\"section-title\">个人信息</view>\r\n" + //
                        "        <view wx:if=\"{{userInfo.phone || resumeData.personalInfo.phone || resumeData.contact.phone}}\" class=\"contact-item\"><text class=\"contact-label\">电话：</text><text class=\"contact-value\">{{userInfo.phone || resumeData.personalInfo.phone || resumeData.contact.phone}}</text></view>\r\n" + //
                        "        <view wx:if=\"{{userInfo.email || resumeData.personalInfo.email || resumeData.contact.email}}\" class=\"contact-item\"><text class=\"contact-label\">邮箱：</text><text class=\"contact-value\">{{userInfo.email || resumeData.personalInfo.email || resumeData.contact.email}}</text></view>\r\n" + //
                        "        <view wx:if=\"{{userInfo.address || resumeData.personalInfo.address || resumeData.contact.address}}\" class=\"contact-item\"><text class=\"contact-label\">地址：</text><text class=\"contact-value\">{{userInfo.address || resumeData.personalInfo.address || resumeData.contact.address}}</text></view>\r\n" + //
                        "        <view wx:if=\"{{userInfo.birthDate || resumeData.personalInfo.birthDate || resumeData.birth}}\" class=\"contact-item\"><text class=\"contact-label\">出生日期：</text><text class=\"contact-value\">{{userInfo.birthDate || resumeData.personalInfo.birthDate || resumeData.birth}}</text></view>\r\n" + //
                        "        <view wx:if=\"{{resumeData.personalInfo.expectedSalary || resumeData.salary}}\" class=\"contact-item\"><text class=\"contact-label\">期望薪资：</text><text class=\"contact-value\">{{resumeData.personalInfo.expectedSalary || resumeData.salary}}</text></view>\r\n" + //
                        "        <view wx:if=\"{{resumeData.personalInfo.startTime || resumeData.entryTime}}\" class=\"contact-item\"><text class=\"contact-label\">入职时间：</text><text class=\"contact-value\">{{resumeData.personalInfo.startTime || resumeData.entryTime}}</text></view>\r\n" + //
                        "      </view>\r\n" + //
                        "\r\n" + //
                        "      <!-- 技能进度条 -->\r\n" + //
                        "      <view class=\"skills-section\">\r\n" + //
                        "        <view class=\"section-title\">专业技能</view>\r\n" + //
                        "        <block wx:for=\"{{resumeData.skillsWithLevel || resumeData.skills}}\" wx:key=\"index\">\r\n" + //
                        "          <view wx:if=\"{{item.name || item}}\" class=\"skill-item\">\r\n" + //
                        "            <view class=\"skill-name\">{{item.name || item}}</view>\r\n" + //
                        "            <view class=\"skill-bar\"><view class=\"skill-fill\" style=\"width:{{item.level * 20}}%;\"></view></view>\r\n" + //
                        "          </view>\r\n" + //
                        "        </block>\r\n" + //
                        "      </view>\r\n" + //
                        "    </view>\r\n" + //
                        "  </view>\r\n" + //
                        "</template>\r\n" + //
                        "";
        String wxss = ".template-one-container {\r\n" + //
                        "  width: 100%;\r\n" + //
                        "  min-height: 100mm;\r\n" + //
                        "  display: flex;\r\n" + //
                        "  font-family: \"PingFang SC\", \"Microsoft YaHei\", Arial, sans-serif;\r\n" + //
                        "  background: #fff;\r\n" + //
                        "  padding: 15rpx 10rpx;\r\n" + //
                        "  box-sizing: border-box;\r\n" + //
                        "  transform: scale(1);\r\n" + //
                        "  transform-origin: top left;\r\n" + //
                        "}\r\n" + //
                        "\r\n" + //
                        "/* 左侧主内容区（原右侧） */\r\n" + //
                        ".template-one-container .left-panel-swapped {\r\n" + //
                        "  width: 70%;\r\n" + //
                        "  min-height: 100mm;\r\n" + //
                        "  padding-right: 15rpx;\r\n" + //
                        "  padding-left: 15rpx;\r\n" + //
                        "  padding-top: 20rpx;\r\n" + //
                        "  padding-bottom: 20rpx;\r\n" + //
                        "  background: #ffffff8e;\r\n" + //
                        "  display: flex;\r\n" + //
                        "  flex-direction: column;\r\n" + //
                        "  border-radius: 10rpx;\r\n" + //
                        "}\r\n" + //
                        "\r\n" + //
                        "/* 右侧信息区（原左侧） */\r\n" + //
                        ".template-one-container .right-panel-swapped {\r\n" + //
                        "  width: 30%;\r\n" + //
                        "  min-height: 100mm;\r\n" + //
                        "  background: #572dca8e;\r\n" + //
                        "  border-radius: 10rpx;\r\n" + //
                        "  padding: 24rpx;\r\n" + //
                        "  color: #fff;\r\n" + //
                        "  display: flex;\r\n" + //
                        "  flex-direction: column;\r\n" + //
                        "  align-items: center;\r\n" + //
                        "  box-shadow: 0 6rpx 12rpx rgba(0,0,0,0.2);\r\n" + //
                        "}\r\n" + //
                        "\r\n" + //
                        "/* 头像与基本信息 */\r\n" + //
                        ".template-one-container .avatar-container {\r\n" + //
                        "  text-align: center;\r\n" + //
                        "  margin-bottom: 30rpx;\r\n" + //
                        "}\r\n" + //
                        ".template-one-container .avatar {\r\n" + //
                        "  width: 140rpx;\r\n" + //
                        "  height: 140rpx;\r\n" + //
                        "  border-radius: 50%;\r\n" + //
                        "  border: 3rpx solid rgba(255,255,255,0.8);\r\n" + //
                        "  margin-bottom: 12rpx;\r\n" + //
                        "}\r\n" + //
                        ".template-one-container .name {\r\n" + //
                        "  font-size: 32rpx;\r\n" + //
                        "  font-weight: 700;\r\n" + //
                        "  margin-bottom: 6rpx;\r\n" + //
                        "}\r\n" + //
                        ".template-one-container .title {\r\n" + //
                        "  font-size: 18rpx;\r\n" + //
                        "  color: #fff;\r\n" + //
                        "  opacity: 0.9;\r\n" + //
                        "}\r\n" + //
                        "\r\n" + //
                        "/* 联系方式 */\r\n" + //
                        ".template-one-container .contact-section {\r\n" + //
                        "  width: 100%;\r\n" + //
                        "  margin-top: 30rpx;\r\n" + //
                        "}\r\n" + //
                        ".template-one-container .contact-section .section-title {\r\n" + //
                        "  font-size: 20rpx;\r\n" + //
                        "  font-weight: 600;\r\n" + //
                        "  margin-bottom: 16rpx;\r\n" + //
                        "  border-bottom: 1rpx solid rgba(255,255,255,0.4);\r\n" + //
                        "  padding-bottom: 4rpx;\r\n" + //
                        "}\r\n" + //
                        "\r\n" + //
                        ".template-one-container .contact-item {\r\n" + //
                        "  font-size: 15rpx;\r\n" + //
                        "  display: flex;\r\n" + //
                        "  flex-wrap: wrap;\r\n" + //
                        "  line-height: 30rpx;\r\n" + //
                        "  margin-bottom: 10rpx;\r\n" + //
                        "}\r\n" + //
                        ".template-one-container .contact-label {\r\n" + //
                        "  font-weight: 500;\r\n" + //
                        "  margin-right: 4rpx;\r\n" + //
                        "  white-space: nowrap;\r\n" + //
                        "}\r\n" + //
                        ".template-one-container .contact-value {\r\n" + //
                        "  flex: 1;\r\n" + //
                        "  word-break: break-word;\r\n" + //
                        "}\r\n" + //
                        "\r\n" + //
                        "/* 技能栏 */\r\n" + //
                        ".template-one-container .skills-section {\r\n" + //
                        "  width: 100%;\r\n" + //
                        "  margin-top: 40rpx;\r\n" + //
                        "}\r\n" + //
                        ".template-one-container .skill-item {\r\n" + //
                        "  margin-bottom: 12rpx;\r\n" + //
                        "}\r\n" + //
                        ".template-one-container .skill-name {\r\n" + //
                        "  font-size: 16rpx;\r\n" + //
                        "  margin-bottom: 4rpx;\r\n" + //
                        "}\r\n" + //
                        ".template-one-container .skill-bar {\r\n" + //
                        "  width: 100%;\r\n" + //
                        "  height: 10rpx;\r\n" + //
                        "  background: rgba(255,255,255,0.3);\r\n" + //
                        "  border-radius: 8rpx;\r\n" + //
                        "  overflow: hidden;\r\n" + //
                        "}\r\n" + //
                        ".template-one-container .skill-fill {\r\n" + //
                        "  height: 100%;\r\n" + //
                        "  background: #fff;\r\n" + //
                        "}\r\n" + //
                        "\r\n" + //
                        "/* Section 通用样式 */\r\n" + //
                        ".template-one-container .section {\r\n" + //
                        "  background: #ffffff;\r\n" + //
                        "  border-radius: 12rpx;\r\n" + //
                        "  padding: 16rpx;\r\n" + //
                        "  margin-bottom: 16rpx;\r\n" + //
                        "  box-shadow: 0 4rpx 8rpx rgba(0,0,0,0.08);\r\n" + //
                        "}\r\n" + //
                        ".template-one-container .section-title {\r\n" + //
                        "  font-size: 20rpx;\r\n" + //
                        "  font-weight: 600;\r\n" + //
                        "  margin-bottom: 20rpx;\r\n" + //
                        "  border-left: 6rpx solid #148291;\r\n" + //
                        "  padding-left: 10rpx;\r\n" + //
                        "}\r\n" + //
                        ".template-one-container .job {\r\n" + //
                        "  margin-bottom: 10rpx;\r\n" + //
                        "}\r\n" + //
                        ".template-one-container .job-header {\r\n" + //
                        "  font-size: 15rpx;\r\n" + //
                        "  font-weight: 600;\r\n" + //
                        "  display: flex;\r\n" + //
                        "  justify-content: space-between;\r\n" + //
                        "  margin-bottom: 10rpx;\r\n" + //
                        "  color: #333;\r\n" + //
                        "}\r\n" + //
                        ".template-one-container .job-desc {\r\n" + //
                        "  font-size: 16rpx;\r\n" + //
                        "  line-height: 1.5;\r\n" + //
                        "  color: #444;\r\n" + //
                        "  margin-bottom: 10rpx;\r\n" + //
                        "}\r\n" + //
                        ".template-one-container .section .job-desc::before {\r\n" + //
                        "  content: \"·\";\r\n" + //
                        "  color: #000;\r\n" + //
                        "  font-weight: bold;\r\n" + //
                        "  margin-right: 6rpx;\r\n" + //
                        "}\r\n" + //
                        "";

        Map<String, Object> result = new HashMap<>();
        result.put("userInfo", new HashMap<String, Object>() {{
                put("name", "张三");
                put("email", "zhangsan@example.com");
                put("phone", "13800000000");
                put("address", "北京市朝阳区");
                put("birthDate", "1990-01-01");
                put("avatarUrl", "https://via.placeholder.com/140");
        }});
        List<Map<String, Object>> educationList = new ArrayList<>();
        Map<String, Object> education = new HashMap<>();
        education.put("school", "清华大学");
        education.put("degree", "本科");
        education.put("startDate", "2010");
        education.put("endDate", "2014");
        education.put("major", "计算机科学与技术");
        educationList.add(education);
        result.put("educationList", educationList);
        List<Map<String, Object>> workExperienceList = new ArrayList<>();
        Map<String, Object> workExperience = new HashMap<>();
        workExperience.put("company", "字节跳动");
        workExperience.put("position", "Java开发工程师");
        workExperience.put("startDate", "2017");
        workExperience.put("endDate", "2019");
        workExperience.put("description", "负责后端开发");
        workExperienceList.add(workExperience);
        result.put("workExperienceList", workExperienceList);
        
        List<Map<String, Object>> skillList = new ArrayList<>();
        Map<String, Object> skill1 = new HashMap<>();
        skill1.put("name", "Java");
        skill1.put("level", 5);
        skillList.add(skill1);
        
        Map<String, Object> skill2 = new HashMap<>();
        skill2.put("name", "Spring Boot");
        skill2.put("level", 4);
        skillList.add(skill2);
        result.put("skillList", skillList);
        result.put("jobTitle", "后端开发工程师");
        result.put("expectedSalary", "15k-25k");
        result.put("startTime", "2025-12-01");
        result.put("selfEvaluation", "负责且踏实");
        result.put("interests", "篮球、旅行");

        try {
            String out = util.generatePdfFromWxml(wxml, wxss, result, "D:\\temp_pdf");
            System.out.println("PDF 已生成: " + out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
