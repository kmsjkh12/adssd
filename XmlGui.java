package CDCD;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class XmlGui extends JFrame {
	
	public XmlGui() {
	        setTitle("개발자 유틸리티 v1.2");
	        setSize(900, 700);
	        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	        setLocationRelativeTo(null);
	        JTabbedPane tabbedPane = new JTabbedPane();
	        tabbedPane.addTab("XML 쿼리 변환", XmlParser.createXmlParserPanel());
	        tabbedPane.addTab("XML ID <-> Java 연결", XmlMapper.createXmlIdFinderPanel());

	        add(tabbedPane);
	    }
	 
	  
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            XmlGui gui = new XmlGui();
            gui.setVisible(true);
        });
    }
}