package CDCD;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XmlParser {

	 static JPanel createXmlParserPanel() {
	        JPanel panel = new JPanel(new BorderLayout(10, 10));
	        JTextArea xmlInputArea = new JTextArea();
	        xmlInputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
	        JTextArea sqlOutputArea = new JTextArea();
	        sqlOutputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
	        sqlOutputArea.setEditable(false);
	        sqlOutputArea.setBackground(new Color(240, 240, 240));
	        JScrollPane inputScrollPane = new JScrollPane(xmlInputArea);
	        JScrollPane outputScrollPane = new JScrollPane(sqlOutputArea);
	        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, inputScrollPane, outputScrollPane);
	        splitPane.setResizeWeight(0.5);
	        JButton parseButton = new JButton("▶ 쿼리 변환 ▶");
	        parseButton.addActionListener(e -> {
	            String xmlFragment = xmlInputArea.getText();
	            if (xmlFragment == null || xmlFragment.trim().isEmpty()) {
	                JOptionPane.showMessageDialog(panel, "변환할 XML 코드를 입력하세요.", "경고", JOptionPane.WARNING_MESSAGE);
	                return;
	            }
	            String fullXmlDocument = "<dummy-root>" + xmlFragment + "</dummy-root>";
	            Map<String, String> data = parseQueryAndComment(fullXmlDocument);
	            if (data.containsKey("error")) {
	                sqlOutputArea.setForeground(Color.RED);
	                sqlOutputArea.setText("오류가 발생했습니다:\n" + data.get("error"));
	            } else {
	                sqlOutputArea.setForeground(Color.BLACK);
	                sqlOutputArea.setText(data.get("query"));
	            }
	        });
	        panel.add(splitPane, BorderLayout.CENTER);
	        panel.add(parseButton, BorderLayout.SOUTH);
	        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	        return panel;
	    }
	 public static Map<String, String> parseQueryAndComment(String xmlContent) {
	        Map<String, String> result = new HashMap<>();
	        StringBuilder rawQueryBuilder = new StringBuilder();
	        try {
	            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	            DocumentBuilder builder = factory.newDocumentBuilder();
	            Document doc = builder.parse(new InputSource(new StringReader(xmlContent)));
	            Node rootNode = doc.getDocumentElement();
	            NodeList children = rootNode.getChildNodes();
	            for (int i = 0; i < children.getLength(); i++) {
	                Node child = children.item(i);
	                switch (child.getNodeType()) {
	                    case Node.COMMENT_NODE: break;
	                    case Node.CDATA_SECTION_NODE: case Node.TEXT_NODE:
	                        rawQueryBuilder.append(child.getTextContent());
	                        break;
	                    case Node.ELEMENT_NODE:
	                        rawQueryBuilder.append(parseDynamicTags(child));
	                        break;
	                }
	            }
	        } catch (ParserConfigurationException | SAXException | IOException e) {
	            result.put("error", "XML 파싱 중 오류 발생: " + e.getMessage());
	            return result;
	        }
	        String finalQuery = rawQueryBuilder.toString().replaceAll("#\\{[^}]+}", "''").replaceAll("\\s+", " ").trim();
	        result.put("query", finalQuery);
	        return result;
	    }
	 private static String parseDynamicTags(Node node) {
	        StringBuilder sb = new StringBuilder();
	        String tagName = node.getNodeName().toLowerCase();
	        if ("where".equals(tagName)) {
	            String innerContent = getInnerContent(node).trim();
	            if (!innerContent.isEmpty()) {
	                innerContent = innerContent.replaceFirst("^(?i)(AND|OR)\\s+", "");
	                return " WHERE " + innerContent;
	            }
	            return "";
	        }
	        if ("choose".equals(tagName)) {
	            NodeList options = node.getChildNodes();
	            for (int i = 0; i < options.getLength(); i++) {
	                Node opt = options.item(i);
	                if (opt.getNodeType() == Node.ELEMENT_NODE) {
	                    String optName = opt.getNodeName().toLowerCase();
	                    if ("when".equals(optName) || "otherwise".equals(optName)) {
	                        sb.append(getInnerContent(opt));
	                        break;
	                    }
	                }
	            }
	            return sb.toString();
	        }
	        if ("foreach".equals(tagName)) { return "('')"; }
	        return getInnerContent(node);
	    }
	 private static String getInnerContent(Node node) {
	        StringBuilder sb = new StringBuilder();
	        NodeList children = node.getChildNodes();
	        for (int i = 0; i < children.getLength(); i++) {
	            Node child = children.item(i);
	            if (child.getNodeType() == Node.TEXT_NODE || child.getNodeType() == Node.CDATA_SECTION_NODE) {
	                sb.append(child.getTextContent());
	            } else if (child.getNodeType() == Node.ELEMENT_NODE) {
	                sb.append(parseDynamicTags(child));
	            }
	        }
	        return sb.toString();
	    }
	 
}
