package com.huawei.fin.ai.material.material.tool;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xslf.usermodel.XSLFGroupShape;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTableCell;
import org.apache.poi.xslf.usermodel.XSLFTableRow;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

import com.huawei.fin.ai.material.common.exception.MaterialErrorCode;
import com.huawei.fin.ai.material.common.exception.MaterialException;
import com.huawei.fin.ai.material.common.util.FileUtil;
import com.huawei.fin.ai.material.common.util.JsonUtil;
import com.huawei.fin.ai.material.common.util.TextUtil;
import com.huawei.fin.ai.material.material.vo.MaterialParseResultVO;
import com.huawei.fin.ai.material.material.vo.MaterialSlideVO;

@Component
public class PptParseTool {

    public MaterialParseResultVO parse(Path file) {
        String ext = FileUtil.extension(file.getFileName().toString());
        try {
            List<MaterialSlideVO> slides = switch (ext) {
                case "pptx" -> parsePptx(file);
                case "docx" -> parseDocx(file);
                case "xlsx" -> parseXlsx(file);
                case "pdf" -> parsePdf(file);
                case "ppt", "doc", "xls" -> throw new MaterialException(
                        MaterialErrorCode.FILE_ERROR, "暂不支持旧版 Office 格式: " + ext);
                default -> parsePlainText(file);
            };
            MaterialParseResultVO result = new MaterialParseResultVO();
            result.setSlides(slides);
            return result;
        } catch (MaterialException e) {
            throw e;
        } catch (Exception e) {
            throw new MaterialException(MaterialErrorCode.FILE_ERROR, "材料解析失败: " + e.getMessage());
        }
    }

    private List<MaterialSlideVO> parsePptx(Path file) throws Exception {
        List<MaterialSlideVO> slides = new ArrayList<>();
        try (FileInputStream in = new FileInputStream(file.toFile());
             XMLSlideShow ppt = new XMLSlideShow(in)) {
            int no = 0;
            for (XSLFSlide slide : ppt.getSlides()) {
                no++;
                List<String> texts = new ArrayList<>();
                List<List<List<String>>> tables = new ArrayList<>();
                for (XSLFShape shape : slide.getShapes()) {
                    collectShapes(shape, texts, tables);
                }
                String raw = String.join("\n", texts);
                slides.add(buildSlide(no, raw, Map.of("texts", texts, "tables", tables)));
            }
        }
        return slides;
    }

    private void collectShapes(XSLFShape shape, List<String> texts, List<List<List<String>>> tables) {
        if (shape instanceof XSLFTextShape textShape) {
            String text = textShape.getText();
            if (text != null && !text.isBlank()) {
                texts.add(text.trim());
            }
        } else if (shape instanceof XSLFTable table) {
            List<List<String>> rows = new ArrayList<>();
            for (XSLFTableRow row : table.getRows()) {
                List<String> cells = new ArrayList<>();
                for (XSLFTableCell cell : row.getCells()) {
                    cells.add(cell.getText());
                }
                rows.add(cells);
            }
            tables.add(rows);
        } else if (shape instanceof XSLFGroupShape group) {
            for (XSLFShape child : group.getShapes()) {
                collectShapes(child, texts, tables);
            }
        }
    }

    private List<MaterialSlideVO> parseDocx(Path file) throws Exception {
        List<MaterialSlideVO> slides = new ArrayList<>();
        try (FileInputStream in = new FileInputStream(file.toFile());
             XWPFDocument doc = new XWPFDocument(in)) {
            List<String> lines = new ArrayList<>();
            for (XWPFParagraph paragraph : doc.getParagraphs()) {
                if (!paragraph.getText().isBlank()) {
                    lines.add(paragraph.getText().trim());
                }
            }
            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    List<String> cells = new ArrayList<>();
                    for (XWPFTableCell cell : row.getTableCells()) {
                        cells.add(cell.getText().trim());
                    }
                    lines.add(String.join(" | ", cells));
                }
            }
            String raw = String.join("\n", lines);
            slides.add(buildSlide(1, raw, Map.of("texts", lines)));
        }
        return slides;
    }

    private List<MaterialSlideVO> parseXlsx(Path file) throws Exception {
        List<MaterialSlideVO> slides = new ArrayList<>();
        try (FileInputStream in = new FileInputStream(file.toFile());
             XSSFWorkbook workbook = new XSSFWorkbook(in)) {
            int no = 0;
            for (Sheet sheet : workbook) {
                no++;
                List<String> lines = new ArrayList<>();
                for (Row row : sheet) {
                    List<String> cells = new ArrayList<>();
                    for (Cell cell : row) {
                        cells.add(cell.toString());
                    }
                    lines.add(String.join(" | ", cells));
                }
                String raw = String.join("\n", lines);
                slides.add(buildSlide(no, raw, Map.of("texts", lines)));
            }
        }
        return slides;
    }

    private List<MaterialSlideVO> parsePdf(Path file) throws Exception {
        List<MaterialSlideVO> slides = new ArrayList<>();
        try (PDDocument document = PDDocument.load(file.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(document);
                slides.add(buildSlide(page, text, Map.of("texts", List.of(text))));
            }
        }
        return slides;
    }

    private List<MaterialSlideVO> parsePlainText(Path file) throws Exception {
        String text = Files.readString(file);
        List<MaterialSlideVO> slides = new ArrayList<>();
        slides.add(buildSlide(1, text, Map.of("texts", List.of(text))));
        return slides;
    }

    private MaterialSlideVO buildSlide(int no, String raw, Map<String, Object> structured) {
        MaterialSlideVO slide = new MaterialSlideVO();
        slide.setSlideNo(no);
        slide.setTitle(TextUtil.extractFirstLine(raw));
        slide.setRawText(raw);
        slide.setStructuredContent(JsonUtil.toJson(structured));
        slide.setParseStatus("PARSED");
        return slide;
    }
}
