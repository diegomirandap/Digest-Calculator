import java.io.*;
import java.security.*;

public class DigestCalculator{

    public static void main (String[] args) throws Exception{
        if (args.length != 3) {
            System.err.println("Usage: java DigestCalculator <Tipo_Digest> <Caminho_da_Pasta_dos_Arquivos> <Caminho_ArqListaDigest>");
            System.exit(1);
        }
        String tipoDigest = args[0];
        File pasta = new File(args[1]);
        File arqListaDigest = new File(args[2]);

        //carregar lista de digests
        // listaDigests = ;

        File[] arquivos = pasta.listFiles();
        if (arquivos != null) {
            for (File arquivo : arquivos) {
                //Para cada um dos arquivos dentro do path fornecido
                if (arquivo.isFile()) {
                    //Calcula o digest
                    String digestCalculado = calcularDigest(arquivo, tipoDigest);
                    //Verifica a compatibilidade do calculado com o fornecido
                    String status = compararDigest(arquivo.getName(), digestCalculado, tipoDigest, listaDigests);
                    //
                    System.out.println(arquivo.getName() + " " + tipoDigest + " " + digestCalculado + " (" + status + ")");
                    if (status.equals("NOT FOUND")) {
                        adicionarDigest(arquivo.getName(), tipoDigest, digestCalculado, listaDigests, arqListaDigest);
                    }
                }
            }
        }
    }

    private static String calcularDigest(File arquivo, String tipoDigest) throws Exception {

    }

    private static void carregarDigests(File arquivo) throws Exception {
        //private static void carregarDigests() pois ainda nao sei o tipo que vai retornar
    }

    private static String compararDigest(String fileName, String digestCalculado, String tipoDigest, void listaDigests){
        //void listaDigests pois ainda nao sei o tipo que vai armazenar
    }

    private static void adicionarDigest(String fileName, String tipoDigest, String digestCalculado, void listaDigests, File arquivoListaDigest) throws Exception {
        //void listaDigests pois ainda nao sei o tipo que vai armazenar
    }
}