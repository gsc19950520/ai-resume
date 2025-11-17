# HTML转PDF编译错误修复

## 问题描述
`convertHtmlToPdf` 方法存在编译错误，主要问题是：
1. 使用了不存在的 `PdfRendererBuilder` 类
2. 缺少必要的PDF生成依赖

## 修复方案

### 1. 添加缺失的PDF依赖
在 `pom.xml` 中添加 OpenPDF 依赖：
```xml
<!-- PDFBox PDF生成器 -->
<dependency>
    <groupId>com.github.librepdf</groupId>
    <artifactId>openpdf</artifactId>
    <version>1.3.30</version>
</dependency>
```

### 2. 添加必要的import语句
在 `ResumeServiceImpl.java` 中添加以下 import：
```java
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.html.simpleparser.HTMLWorker;
import java.io.StringReader;
```

### 3. 替换PDF生成实现
将原有的 `PdfRendererBuilder` 实现替换为使用 OpenPDF 的实现：

```java
public byte[] convertHtmlToPdf(String htmlContent) { 
    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) { 
        // 创建PDF文档
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter.getInstance(document, os);
        document.open();
        
        // 使用HTMLWorker解析HTML内容
        HTMLWorker htmlWorker = new HTMLWorker(document);
        htmlWorker.parse(new StringReader(htmlContent));
        
        document.close();
        return os.toByteArray(); 
    } catch (DocumentException | IOException e) { 
        throw new RuntimeException("HTML 转 PDF 失败", e); 
    } 
}
```

## 修复后的代码

**修改后的 convertHtmlToPdf 方法：**
```java
public byte[] convertHtmlToPdf(String htmlContent) { 
    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) { 
        // 创建PDF文档
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter.getInstance(document, os);
        document.open();
        
        // 使用HTMLWorker解析HTML内容
        HTMLWorker htmlWorker = new HTMLWorker(document);
        htmlWorker.parse(new StringReader(htmlContent));
        
        document.close();
        return os.toByteArray(); 
    } catch (DocumentException | IOException e) { 
        throw new RuntimeException("HTML 转 PDF 失败", e); 
    } 
}
```

## 技术说明

### 为什么选择 OpenPDF？
1. **LGPL许可**：比iText更友好的开源许可
2. **兼容性**：与原有的iText API兼容
3. **轻量级**：相比其他PDF库更轻量
4. **HTML支持**：通过HTMLWorker支持HTML内容解析

### HTMLWorker 使用说明
- 支持基本的HTML标签（p, h1-h6, div, span等）
- 支持简单的CSS样式
- 适合处理简历模板等结构化HTML内容
- 对于复杂样式可能需要额外的CSS处理

## 测试验证

创建测试类验证修复效果：
```java
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.html.simpleparser.HTMLWorker;
import java.io.*;

public class TestPdfConversion {
    
    public static byte[] convertHtmlToPdf(String htmlContent) { 
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) { 
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, os);
            document.open();
            
            HTMLWorker htmlWorker = new HTMLWorker(document);
            htmlWorker.parse(new StringReader(htmlContent));
            
            document.close();
            return os.toByteArray(); 
        } catch (DocumentException | IOException e) { 
            throw new RuntimeException("HTML 转 PDF 失败", e); 
        } 
    }
    
    public static void main(String[] args) {
        System.out.println("Testing PDF conversion...");
        
        try {
            String htmlContent = "<html><body><h1>测试PDF生成</h1><p>这是一个测试段落。</p></body></html>";
            
            byte[] pdfBytes = convertHtmlToPdf(htmlContent);
            
            System.out.println("PDF generated successfully!");
            System.out.println("PDF size: " + pdfBytes.length + " bytes");
            
            // 保存到文件进行验证
            try (FileOutputStream fos = new FileOutputStream("test.pdf")) {
                fos.write(pdfBytes);
                System.out.println("PDF saved to test.pdf");
            }
            
            System.out.println("✅ PDF conversion test completed successfully!");
            
        } catch (Exception e) {
            System.err.println("❌ PDF conversion test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

## 注意事项

1. **依赖管理**：添加 OpenPDF 依赖后需要重新编译项目
2. **HTML兼容性**：HTMLWorker支持基本的HTML标签，复杂样式可能需要预处理
3. **中文字体**：处理中文内容时可能需要额外配置字体
4. **性能优化**：对于大量PDF生成，考虑使用连接池或缓存机制

## 结论

通过替换 `PdfRendererBuilder` 为 OpenPDF 的 `HTMLWorker` 实现，成功修复了编译错误。新的实现提供了：
- ✅ 稳定的PDF生成功能
- ✅ 基本的HTML解析支持
- ✅ 与Spring Boot兼容的依赖管理
- ✅ 适合简历模板等结构化内容处理