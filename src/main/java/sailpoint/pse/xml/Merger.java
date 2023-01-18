package sailpoint.pse.xml;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Merger {

	private File baseFolder = null;
	private DocumentBuilderFactory factory = null;

	public Merger(File baseFolder) throws ParserConfigurationException {
		System.err.println("Merger - initializing");
		this.baseFolder = baseFolder;
		factory = DocumentBuilderFactory.newInstance();
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		factory.setValidating(false);
		factory.setNamespaceAware(true);
		factory.setFeature("http://xml.org/sax/features/namespaces", false);
		factory.setFeature("http://xml.org/sax/features/validation", false);
		factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
		factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
	}

	private List<String> findXmlFiles(File base) {
		List<String> fileNames = new ArrayList<String>();
		if (base != null && base.isDirectory() && base.canRead()) {
			File[] files = base.listFiles();
			if (files != null) {
				for (File file : files) {
					if (file.getName().equals(".") || file.getName().equals("..")) {
						continue;
					}
					if (file.isDirectory()) {
						List<String> subFiles = findXmlFiles(file);
						if (subFiles != null && !subFiles.isEmpty()) {
							fileNames.addAll(subFiles);
						}
					} else {
						if (file.getName().toLowerCase().endsWith(".xml")) {
							fileNames.add(file.getAbsolutePath());
						}
					}
				}
			}
		}
		return fileNames;
	}

	private List<Element> processXmlFile(String fileToProcess) {
		List<Element> result = new ArrayList<Element>();
		try {
			File file = new File(fileToProcess);
			if (!file.isDirectory() && file.canRead()) {
				DocumentBuilder db = factory.newDocumentBuilder();
				Document doc = db.parse(file);
				doc.getDocumentElement().normalize();
				String rootElementName = doc.getDocumentElement().getNodeName();
				if ("sailpoint".equals(rootElementName)) {
					// Multiple children expected
					NodeList childNodes = doc.getDocumentElement().getChildNodes();
					if (childNodes != null && childNodes.getLength() > 0) {
						for (int c = 0; c < childNodes.getLength(); c++) {
							Node node = childNodes.item(c);
							if (node instanceof Element) {
								result.add((Element) node);
							}
						}
					}
				} else {
					result.add(doc.getDocumentElement());
				}
			}
		} catch (ParserConfigurationException | SAXException | IOException e) {
			System.err.println(String.format("Error processing file '%s': %s ", fileToProcess, e.getMessage()));
			e.printStackTrace();
		} finally {
		}
		return result;
	}

	public void execute() throws TransformerException, ParserConfigurationException, SAXException, IOException {
		if (baseFolder != null) {
			List<String> filesToProcess = findXmlFiles(baseFolder);
			DocumentBuilder db = factory.newDocumentBuilder();
			Document document = db.newDocument();
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			DOMImplementation domImpl = document.getImplementation();
			DocumentType doctype = domImpl.createDocumentType("sailpoint", "sailpoint.dtd", "sailpoint.dtd");
			transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, doctype.getPublicId());
			transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctype.getSystemId());
			Element root = document.createElement("sailpoint");
			document.appendChild(root);
			for (String fileToProcess : filesToProcess) {
				List<Element> elements = processXmlFile(fileToProcess);
				if (elements != null && !elements.isEmpty()) {
					for (Element element : elements) {
						Node node = document.importNode(element, true);
						root.appendChild(node);
					}
				}
			}
			StringWriter writer = new StringWriter();
			document.normalize();
			transformer.transform(new DOMSource(document), new StreamResult(writer));
			System.out.print(writer.getBuffer().toString());
		}
	}

	public static void main(String[] args) throws TransformerException, ParserConfigurationException, SAXException, IOException {
		if (args != null && args.length > 0) {
			if (args.length != 1) {
				System.err.println("Only 1 argument expected: base folder");
				System.exit(1);
			}
			String folder = args[0];
			File baseFolder = new File(folder);
			if (!baseFolder.isDirectory()) {
				System.err.println(String.format("Argument %s is not a folder", folder));
				System.exit(2);
			}
			if (!baseFolder.canRead()) {
				System.err.println(String.format("Folder %s is not readable", folder));
				System.exit(3);
			}
			Merger merger = new Merger(baseFolder);
			merger.execute();
		} else {
			System.err.println("Base folder as argument required");
			System.exit(1);
		}
	}

}
