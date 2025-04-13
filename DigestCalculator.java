// java DigestCalculator.java "SHA-256" "arquivos/" "lista.xml"
import java.io.File;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class DigestCalculator{

    public static void main (String[] args) throws Exception{
        if (args.length != 3) {
            System.err.println("Usage: java DigestCalculator <Tipo_Digest> <Caminho_da_Pasta_dos_Arquivos> <Caminho_ArqListaDigest>");
            System.exit(1);
        }
        String tipoDigest = args[0]; //(MD5/SHA1/SHA256/SHA512)
        File pasta = new File(args[1]);
        File arqListaDigest = new File(args[2]);

        //Carregar lista de digests
        Map<String, Map<String, String>> listaDigests = carregarDigests(arqListaDigest);
        //hash map para mapear cada um dos arquivos, seus tipos de digest e o digest calculado
        //Map<FILE_NAME, Map<DIGEST_TYPE, DIGEST_HEX>>

        File[] arquivos = pasta.listFiles();
        if (arquivos != null) {
            for (File arquivo : arquivos) {
                //Para cada um dos arquivos dentro do path fornecido
                if (arquivo.isFile()) {
                    //Calcula o digest
                    String digestCalculado = calcularDigest(arquivo, tipoDigest);
                    //Verifica a compatibilidade do calculado com o fornecido
                    String status = compararDigest(arquivo.getName(), digestCalculado, tipoDigest, listaDigests);
                    System.out.println(arquivo.getName() + " " + tipoDigest + " " + digestCalculado + " (" + status + ")");
                    if (status.equals("NOT_FOUND")) {
                        adicionarDigest(arquivo.getName(), tipoDigest, digestCalculado, listaDigests, arqListaDigest);
                    }
                }
            }
        }
    }

    private static String calcularDigest(File arquivo, String tipoDigest) throws Exception {
        //substituir args[0] pelo conteudo do arquivo passado
        //String teste = new String(Files.readAllBytes(file.toPath(arquivo)));
        //byte[] plainText = teste.getBytes("UTF8");
        byte[] plainText = Files.readAllBytes(arquivo.toPath());

        MessageDigest messageDigest = MessageDigest.getInstance(tipoDigest);
        //System.out.println( "\n" + messageDigest.getProvider().getInfo() );

        messageDigest.update( plainText);
        byte [] digest = messageDigest.digest();
        //System.out.println( "\nDigest length: " + digest.length * 8 + "bits" );

        // converte o digist para hexadecimal
        StringBuffer buf = new StringBuffer();
        for(int i = 0; i < digest.length; i++) {
            String hex = Integer.toHexString(0x0100 + (digest[i] & 0x00FF)).substring(1);
            buf.append((hex.length() < 2 ? "0" : "") + hex);
        }

        return buf.toString();
    }

    private static Map<String, Map<String, String>> carregarDigests(File arquivo) throws Exception {
        //Map<FILE_NAME, Map<DIGEST_TYPE, DIGEST_HEX>>
        Map<String, Map<String, String>> mapa = new HashMap<>();

        //Preparar parser DOM
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        //Ler o XML como documento
        Document doc = builder.parse(arquivo);
        doc.getDocumentElement().normalize();

        NodeList fileEntries = doc.getElementsByTagName("FILE_ENTRY");
        for (int i = 0; i < fileEntries.getLength(); i++) {
            Element fileEntry = (Element) fileEntries.item(i);

            // Obter nome do arquivo
            String nomeArq = fileEntry.getElementsByTagName("FILE_NAME").item(0).getTextContent().trim();

            //Criar o mapa da segunda camada
            Map<String, String> digests = new HashMap<>();
            NodeList digestEntries = fileEntry.getElementsByTagName("DIGEST_ENTRY");

            for (int j = 0; j < digestEntries.getLength(); j++) {
                Element digestEntry = (Element) digestEntries.item(j);

                String tipo = digestEntry.getElementsByTagName("DIGEST_TYPE").item(0).getTextContent().trim();
                String valor = digestEntry.getElementsByTagName("DIGEST_HEX").item(0).getTextContent().trim();

                //Preencher o mapa de digests internos
                digests.put(tipo, valor);
            }

            mapa.put(nomeArq, digests);
        }

        return mapa;
    }

    private static String compararDigest(String fileName, String digestCalculado, String tipoDigest, Map<String, Map<String, String>> listaDigests){
        //Verificar se o nome do arquivo está no XML
        if (listaDigests.containsKey(fileName)) {
            Map<String, String> digestsArq = listaDigests.get(fileName);
            if (digestsArq.containsKey(tipoDigest)) {
                String digestDado = digestsArq.get(tipoDigest);
                if (digestDado.equalsIgnoreCase(digestCalculado)) {
                    return "OK";
                } else {
                    return "NOT OK";
                }
            }

            //Verificar colisão com outros arquivos
            for (Map.Entry<String, Map<String, String>> entrada : listaDigests.entrySet()) {
                String outroArquivo = entrada.getKey();
                if (!outroArquivo.equals(fileName)) {
                    Map<String, String> digestsOutros = entrada.getValue();
                    for (String digest : digestsOutros.values()) {
                        if (digest.equalsIgnoreCase(digestCalculado)) {
                            return "COLISION";
                        }
                    }
                }
            }
        }

        return "NOT_FOUND";
    }

    private static void adicionarDigest(String fileName, String tipoDigest, String digestCalculado, Map<String, Map<String, String>> listaDigests, File arquivoListaDigest) throws Exception {

        //Abrir o XML com DOM
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(arquivoListaDigest);
        //doc.getDocumentElement().normalize();

        //Pegar o elemento raiz <CATALOG>
        Element catalog = (Element) doc.getElementsByTagName("CATALOG").item(0);

        //Procurar por <FILE_ENTRY> correspondente ao fileName
        NodeList fileEntries = doc.getElementsByTagName("FILE_ENTRY");
        Element fileEntryEncontrado = null;

        for (int i = 0; i < fileEntries.getLength(); i++) {
            Element fileEntry = (Element) fileEntries.item(i);
            String nome = fileEntry.getElementsByTagName("FILE_NAME").item(0).getTextContent().trim();
            if (nome.equals(fileName)) {
                fileEntryEncontrado = fileEntry;
                break;
            }
        }

        //Se não encontrar o <FILE_ENTRY>, cria um novo
        if (fileEntryEncontrado == null) {
            fileEntryEncontrado = doc.createElement("FILE_ENTRY");

            Element fileNameElement = doc.createElement("FILE_NAME");
            fileNameElement.setTextContent(fileName);
            fileEntryEncontrado.appendChild(fileNameElement);

            catalog.appendChild(fileEntryEncontrado);
        }

        //Criar novo <DIGEST_ENTRY>
        Element digestEntry = doc.createElement("DIGEST_ENTRY");

        Element tipoElement = doc.createElement("DIGEST_TYPE");
        tipoElement.setTextContent(tipoDigest);
        digestEntry.appendChild(tipoElement);

        Element valorElement = doc.createElement("DIGEST_HEX");
        valorElement.setTextContent(digestCalculado);
        digestEntry.appendChild(valorElement);

        fileEntryEncontrado.appendChild(digestEntry);

        //Salvar o documento de volta no mesmo arquivo
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(arquivoListaDigest);
        transformer.transform(source, result);

        return;
    }
}