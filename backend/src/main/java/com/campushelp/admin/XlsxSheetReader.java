// 无第三方依赖的 xlsx 读取器：只解析第一张工作表的单元格文本，供后台用户导入使用。
package com.campushelp.admin;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

final class XlsxSheetReader {
    private XlsxSheetReader() {}

    record SheetRow(int rowNumber, Map<Integer, String> cells) {
        String value(int column) {
            return cells.getOrDefault(column, "");
        }
    }

    // read：解压 xlsx 包，读取 sharedStrings 和第一张工作表；不执行宏、不访问外部实体。
    static List<SheetRow> read(InputStream input) throws IOException {
        byte[] sharedBytes = null;
        byte[] sheetBytes = null;
        String sheetName = null;
        try (ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String name = entry.getName();
                if ("xl/sharedStrings.xml".equals(name)) {
                    sharedBytes = zip.readAllBytes();
                } else if (name.startsWith("xl/worksheets/sheet") && name.endsWith(".xml")
                    && (sheetName == null || name.compareTo(sheetName) < 0)) {
                    sheetName = name;
                    sheetBytes = zip.readAllBytes();
                }
            }
        }
        if (sheetBytes == null) throw new IllegalArgumentException("Excel 中没有可读取的工作表");
        List<String> shared = sharedBytes == null ? List.of() : readSharedStrings(sharedBytes);
        return readSheet(sheetBytes, shared);
    }

    private static List<String> readSharedStrings(byte[] bytes) throws IOException {
        Document document = parse(bytes);
        NodeList items = document.getElementsByTagNameNS("*", "si");
        List<String> result = new ArrayList<>(items.getLength());
        for (int i = 0; i < items.getLength(); i++) {
            NodeList texts = ((Element) items.item(i)).getElementsByTagNameNS("*", "t");
            StringBuilder value = new StringBuilder();
            for (int j = 0; j < texts.getLength(); j++) value.append(texts.item(j).getTextContent());
            result.add(value.toString());
        }
        return result;
    }

    private static List<SheetRow> readSheet(byte[] bytes, List<String> shared) throws IOException {
        Document document = parse(bytes);
        NodeList rows = document.getElementsByTagNameNS("*", "row");
        List<SheetRow> result = new ArrayList<>(rows.getLength());
        for (int i = 0; i < rows.getLength(); i++) {
            Element row = (Element) rows.item(i);
            int rowNumber = parseInt(row.getAttribute("r"), i + 1);
            Map<Integer, String> cells = new HashMap<>();
            NodeList children = row.getElementsByTagNameNS("*", "c");
            int fallbackColumn = 0;
            for (int j = 0; j < children.getLength(); j++) {
                Element cell = (Element) children.item(j);
                int column = columnIndex(cell.getAttribute("r"));
                if (column < 0) column = fallbackColumn;
                fallbackColumn = column + 1;
                String value = cellValue(cell, shared).trim();
                if (!value.isEmpty()) cells.put(column, value);
            }
            if (!cells.isEmpty()) result.add(new SheetRow(rowNumber, cells));
        }
        return result;
    }

    private static String cellValue(Element cell, List<String> shared) {
        String type = cell.getAttribute("t");
        if ("inlineStr".equals(type)) {
            return joinTexts(cell, "is");
        }
        Element value = firstChild(cell, "v");
        String raw = value == null ? "" : value.getTextContent();
        if ("s".equals(type)) {
            int index = parseInt(raw, -1);
            return index >= 0 && index < shared.size() ? shared.get(index) : "";
        }
        if ("str".equals(type) || "b".equals(type)) return raw;
        if (raw == null || raw.isBlank()) return "";
        try {
            BigDecimal number = new BigDecimal(raw.trim());
            if (number.scale() > 0) return number.stripTrailingZeros().toPlainString();
            return number.toBigInteger().toString();
        } catch (NumberFormatException ignored) {
            return raw;
        }
    }

    private static String joinTexts(Element parent, String containerName) {
        Element container = firstChild(parent, containerName);
        if (container == null) return "";
        NodeList texts = container.getElementsByTagNameNS("*", "t");
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < texts.getLength(); i++) value.append(texts.item(i).getTextContent());
        return value.toString();
    }

    private static Element firstChild(Element parent, String localName) {
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeType() == Node.ELEMENT_NODE && localName.equals(node.getLocalName())) {
                return (Element) node;
            }
        }
        return null;
    }

    private static Document parse(byte[] bytes) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            return factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
        } catch (Exception error) {
            throw new IOException("Excel 文件结构无法解析", error);
        }
    }

    private static int columnIndex(String reference) {
        if (reference == null || reference.isBlank()) return -1;
        int column = 0;
        for (int i = 0; i < reference.length(); i++) {
            char ch = Character.toUpperCase(reference.charAt(i));
            if (ch < 'A' || ch > 'Z') break;
            column = column * 26 + (ch - 'A' + 1);
        }
        return column == 0 ? -1 : column - 1;
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
