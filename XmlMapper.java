package CDCD;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import java.awt.*;
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
import java.util.stream.Stream;

public class XmlMapper {
	private static final Set<String> EXCLUDED_DIRS = new HashSet<>(Arrays.asList(
	        "resources", "target", "build", ".git", ".idea", ".vscode", "node_modules"
	    ));

	 static class FinalResult {
	        final List<SearchResultItem> fileSearchResults;
	        FinalResult(List<SearchResultItem> fileSearchResults) {
	            this.fileSearchResults = fileSearchResults;
	        }
	    }
	
	 
	static JPanel createXmlIdFinderPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        JTextField directoryField = new JTextField(40);
        JButton browseButton = new JButton("찾아보기...");
        topPanel.add(new JLabel("검색할 프로젝트 폴더:"), BorderLayout.WEST);
        topPanel.add(directoryField, BorderLayout.CENTER);
        topPanel.add(browseButton, BorderLayout.EAST);
        JTextArea xmlInputArea = new JTextArea();
        xmlInputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JTextArea resultArea = new JTextArea();
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        resultArea.setEditable(false);
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(xmlInputArea), new JScrollPane(resultArea));
        splitPane.setResizeWeight(0.4);
        JButton findButton = new JButton("연결된 파일 및 파라미터 검증");
        browseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION) {
                directoryField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        findButton.addActionListener(e -> {
            String directory = directoryField.getText();
            String xmlFragment = xmlInputArea.getText();
            if (directory.trim().isEmpty() || xmlFragment.trim().isEmpty()) {
                JOptionPane.showMessageDialog(panel, "프로젝트 폴더와 XML 코드를 모두 입력하세요.", "경고", JOptionPane.WARNING_MESSAGE);
                return;
            }
            resultArea.setText("찾는 중 \n");
            findButton.setEnabled(false);
            SwingWorker<FinalResult, Void> worker = new SwingWorker<>() {
                @Override
                protected FinalResult doInBackground() throws Exception {
                	Map<String, String> keywordBlockMap = extractAllKeywordsAndBlocks(xmlFragment);
                    
                    if (keywordBlockMap.isEmpty()) {
                        throw new Exception("XML에서 주석이 없습니다");
                    }

                    // 2. [변경] 디렉토리를 한 번만 훑어서, 맵에 있는 모든 키워드를 한꺼번에 찾습니다.
                    List<SearchResultItem> resultList = searchAllFilesOnce(directory, keywordBlockMap);

                    return new FinalResult(resultList);
                }

                @Override
                protected void done() {
                    try {
                        FinalResult finalResult = get();
                        List<SearchResultItem> fileSearchResults = finalResult.fileSearchResults;

                        StringBuilder sb = new StringBuilder();
                        sb.append("▶ 분석 완료 (총 ").append(fileSearchResults.size()).append("건 매칭)\n");

                        if (fileSearchResults.isEmpty()) {
                            sb.append("\n 키워드 파일이 없습니다. 수동으로 이클립스에서 찾으세요 \n");
                        } else {
                            for (SearchResultItem item : fileSearchResults) {
                                sb.append("\n==================================================\n");
                                sb.append("[키워드: ").append(item.keyword).append("]\n");
                                sb.append("FILE: ").append(item.filePath).append("\n");
                                
                                // 코드 출력
                                for (String code : item.codes) {
                                    sb.append("---------------- Code ----------------\n");
                                    sb.append(code).append("\n");
                                }
                                
                                // [검증 결과 출력]
                                if (item.validationResult != null) {
                                    sb.append("---------------- Validation ----------------\n");
                                    // ValidationResult의 toString()이 에러 메시지를 반환한다고 가정
                                    String vResult = item.validationResult.toString();
                                    if (vResult.trim().isEmpty()) {
                                        sb.append("✅ 파라미터 검증 성공 (일치함)\n");
                                    } else {
                                        sb.append("❌ 검증 실패:\n").append(vResult).append("\n");
                                    }
                                }
                            }
                            sb.append("==================================================\n");
                        }
                        
                        resultArea.setText(sb.toString());

                    } catch (Exception ex) {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        resultArea.setText("오류가 발생했습니다:\n" + cause.getMessage());
                        cause.printStackTrace();
                    } finally {
                        findButton.setEnabled(true);
                        resultArea.setCaretPosition(0);
                    }
                }
            };
            worker.execute();
        });
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(splitPane, BorderLayout.CENTER);
        panel.add(findButton, BorderLayout.SOUTH);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return panel;
    }
	public static ValidationResult checkParameters(String xmlQuery, String methodCode) {
        ValidationResult result = new ValidationResult();
        Set<String> xmlParams = new HashSet<>();
        Pattern xmlPattern = Pattern.compile("#\\{([^}]+)\\}");
        Matcher xmlMatcher = xmlPattern.matcher(xmlQuery);
        while (xmlMatcher.find()) {
            String paramName = xmlMatcher.group(1).trim();
            if (paramName.isEmpty()) {
                result.addInvalidPlaceholder(xmlMatcher.group(0));
            } else {
                xmlParams.add(paramName.split(",")[0].trim());
            }
        }
        Set<String> javaParams = new HashSet<>();
        Pattern javaMethodPattern = Pattern.compile("\\.put\\s*\\(\\s*\"([^\"]+)\"");
        Matcher javaMethodMatcher = javaMethodPattern.matcher(methodCode);
        while (javaMethodMatcher.find()) {
            javaParams.add(javaMethodMatcher.group(1));
        }
        for (String xmlParam : xmlParams) {
            if (!javaParams.contains(xmlParam)) {
                result.addMissingParameter(xmlParam);
            }
        }
        return result;
    }
	private static final Pattern SQL_COMMENT_PATTERN = Pattern.compile("(?s)/\\*\\s*(.*?)\\s*\\*/");
	
	
	 
	 
	 private static List<String> findMethodsInContent(String content, String keyword) {
	        List<String> foundMethods = new ArrayList<>();
	        final Pattern methodStartPattern = Pattern.compile(
	                "(?:(?:public|private|protected|static|final|abstract|synchronized|native|strictfp)\\s+)*" +
	                "[\\w<>\\[\\].,?]+\\s+" + "(\\w+)\\s*" + "\\([^)]*\\)" +
	                "(?:\\s*throws\\s+[\\w\\s,]+)?" + "\\s*\\{"
	        );
	        Matcher matcher = methodStartPattern.matcher(content);
	        while (matcher.find()) {
	            int braceCount = 1;
	            int methodBodyStart = matcher.end();
	            int methodEnd = -1;
	            for (int i = methodBodyStart; i < content.length(); i++) {
	                char ch = content.charAt(i);
	                if (ch == '{') braceCount++;
	                else if (ch == '}') braceCount--;
	                if (braceCount == 0) {
	                    methodEnd = i + 1;
	                    break;
	                }
	            }
	            if (methodEnd != -1) {
	                String fullMethodCode = content.substring(matcher.start(), methodEnd);
	                if (fullMethodCode.contains(keyword)) {
	                    foundMethods.add(fullMethodCode.trim());
	                }
	            }
	        }
	        return foundMethods;
	    }
	
	  private static String nodeToString(Node node) {
	        try {
	            StringWriter sw = new StringWriter();
	            Transformer t = TransformerFactory.newInstance().newTransformer();
	            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
	            t.setOutputProperty(OutputKeys.INDENT, "yes");
	            t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
	            t.transform(new DOMSource(node), new StreamResult(sw));
	            return sw.toString().trim();
	        } catch (Exception ex) {
	            return "Error converting node to string";
	        }
	    }
	  
	  private static Map<String, String> extractAllKeywordsAndBlocks(String fullXmlFragment) {
		    Map<String, String> resultMap = new HashMap<>();
		    if (fullXmlFragment == null || fullXmlFragment.trim().isEmpty()) { return resultMap; }

		    String wrappedXml = "<dummy-root>" + fullXmlFragment + "</dummy-root>";
		    try {
		        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		        DocumentBuilder builder = factory.newDocumentBuilder();
		        Document doc = builder.parse(new InputSource(new StringReader(wrappedXml)));
		        
		        Element root = doc.getDocumentElement();
		        NodeList children = root.getChildNodes();

		        // 루트 바로 아래의 자식 노드들(update, select 등)을 하나씩 검사
		        for (int i = 0; i < children.getLength(); i++) {
		            Node node = children.item(i);
		            if (node.getNodeType() == Node.ELEMENT_NODE) {
		                // 이 노드(블록) 안에 SQL 주석 키워드가 있는지 확인
		                String foundKeyword = findKeywordInNode(node);
		                if (foundKeyword != null) {
		                    // 키워드와 해당 XML 블록 전체 문자열을 맵에 저장
		                    resultMap.put(foundKeyword, nodeToString(node));
		                }
		            }
		        }
		    } catch (Exception e) {
		        e.printStackTrace();
		    }
		    return resultMap;
		}
	  
	  private static String findKeywordInNode(Node node) {
		    if (node.getNodeType() == Node.TEXT_NODE || node.getNodeType() == Node.CDATA_SECTION_NODE) {
		        Matcher matcher = SQL_COMMENT_PATTERN.matcher(node.getNodeValue());
		        if (matcher.find()) {
		            return matcher.group(1).trim();
		        }
		    }
		    NodeList children = node.getChildNodes();
		    for (int i = 0; i < children.getLength(); i++) {
		        String found = findKeywordInNode(children.item(i));
		        if (found != null) return found;
		    }
		    return null;
		}
	// [NEW] 파일을 한 번만 읽어서 여러 키워드를 동시에 찾고 검증까지 수행
	  public static List<SearchResultItem> searchAllFilesOnce(String directory, Map<String, String> keywordBlockMap) throws IOException {
	      List<SearchResultItem> totalResults = new ArrayList<>(); // 동시성 문제 방지를 위해 로컬 변수 사용 권장

	      try (Stream<Path> stream = Files.walk(Paths.get(directory))) {
	          stream

	          .filter(path -> {
	                  for (Path part : path) {
	                      if (EXCLUDED_DIRS.contains(part.getFileName().toString())) return false;
	                  }
	                  return true;
	              })
	              	              .filter(path -> path.getFileName().toString().contains("MGMT")) 
	              .filter(path -> !Files.isDirectory(path))
	              .filter(path -> path.toString().endsWith(".java"))
	              
	              // 3. 파일 처리
	              .forEach(path -> {
	                  try {
	                      // 파일 내용을 딱 한 번만 읽음
	                      String content = Files.readString(path);
	                      String filePath = path.toString();

	                      // [핵심] 찾아야 할 모든 키워드에 대해 검사
	                      for (Map.Entry<String, String> entry : keywordBlockMap.entrySet()) {
	                          String keyword = entry.getKey();        // 예: hello_1
	                          String xmlBlock = entry.getValue();     // 예: <update...>

	                          // 이 파일에 해당 키워드가 있는가?
	                          if (content.contains(keyword)) {
	                              // 메서드 추출
	                              List<String> foundMethods = findMethodsInContent(content, keyword);
	                              
	                              // 메서드를 찾았다면 검증 수행 및 결과 저장
	                              if (!foundMethods.isEmpty()) {
	                                  String javaCode = foundMethods.get(0); // 첫 번째 매칭 메서드 사용
	                                  
	                                  // 파라미터 검증
	                                  ValidationResult validation = checkParameters(xmlBlock, javaCode);
	                                  
	                                  // 결과 리스트에 추가
	                                  totalResults.add(new SearchResultItem(
	                                      keyword,
	                                      filePath,
	                                      foundMethods,
	                                      validation
	                                  ));
	                              }
	                          }
	                      }
	                  } catch (IOException e) {
	                      e.printStackTrace();
	                  }
	              });
	      }
	      return totalResults;
	  }
}

