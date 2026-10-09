package template;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

//Toda a mecânica de save foi gerada com o uso de IA, incluindo seu uso dentro do Main.java

/**
 * Tudo que envolve arquivos de save: pasta, listagem, nomes, gravação, leitura e formato.
 * Não conhece a engine nem a tela; o Main só entrega e recebe um {@link Estado}.
 *
 * Formato do arquivo (texto, uma informação por linha; linhas desconhecidas são ignoradas):
 *   JOGO_FABRICA 1
 *   CAM x y zoom
 *   SEL TIPO|NENHUM DIRECAO
 *   C TIPO col row DIRECAO timer indiceSaida     (inicia uma construção)
 *   B item...   itens da esteira (frente primeiro)
 *   E item...   pilha de entrada (base -> topo)
 *   S item...   pilha de saída / estoque do armazém (base -> topo)
 *   F faixa item...   itens de uma faixa do separador
 *   M MODO saidaTipo0 saidaTipo1 ... proximaFaixa   configuração do separador
 *   item = TIPO,progresso,ladoEntrada|-,lateral,faixaDestino
 */
public class GerenciadorSave {

    public static final String EXTENSAO = ".fabrica";

    public static class Estado {
        public double cameraX, cameraY, zoom = 1.0;
        public Construcao.Tipo selecao;                       
        public Esteira.Direcao direcao = Esteira.Direcao.DIREITA;
        public List<Construcao> construcoes = new ArrayList<>();
        public Map<Construcao, List<Item>> itensEsteiras = new HashMap<>(); 
    }

    private final File pasta;
    private final int tileSize, mapaLargura, mapaAltura;

    public GerenciadorSave(String nomePasta, int tileSize, int mapaLargura, int mapaAltura) {
        this.pasta = new File(nomePasta);
        this.tileSize = tileSize;
        this.mapaLargura = mapaLargura;
        this.mapaAltura = mapaAltura;
    }

    public String getNomePasta() { return pasta.getName(); }

    private File garantirPasta() {
        if (!pasta.exists()) pasta.mkdirs();
        return pasta;
    }

    public List<File> listar() {
        List<File> lista = new ArrayList<>();
        File[] arquivos = garantirPasta().listFiles((dir, nome) -> nome.endsWith(EXTENSAO));
        if (arquivos != null) {
            Arrays.sort(arquivos, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
            lista.addAll(Arrays.asList(arquivos));
        }
        return lista;
    }

    public String nomeDe(File f) {
        String n = f.getName();
        return n.endsWith(EXTENSAO) ? n.substring(0, n.length() - EXTENSAO.length()) : n;
    }

    public String dataDe(File f) {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date(f.lastModified()));
    }

    public String sugestaoDeNome() {
        return "save_" + new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date());
    }

    public File arquivoPara(String nomeDigitado) {
        String nome = nomeDigitado.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        if (nome.length() > 60) nome = nome.substring(0, 60).trim();
        if (nome.isEmpty()) nome = sugestaoDeNome();
        return new File(garantirPasta(), nome + EXTENSAO);
    }

    public boolean excluir(File f) { 
        return f.delete(); 
    }

    public void salvar(File f, Estado e) throws IOException {
        String dados = serializar(e);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(f))) {
            writer.write(dados);
        }
    }

    private String serializar(Estado e) {
        StringBuilder sb = new StringBuilder();
        sb.append("JOGO_FABRICA 1\n");
        sb.append("CAM ").append(e.cameraX).append(' ').append(e.cameraY).append(' ').append(e.zoom).append('\n');
        sb.append("SEL ").append(e.selecao == null ? "NENHUM" : e.selecao.name())
          .append(' ').append(e.direcao.name()).append('\n');

        for (Construcao c : e.construcoes) {
            sb.append("C ").append(c.getTipo().name()).append(' ').append(colDe(c)).append(' ').append(rowDe(c))
              .append(' ').append(c.getDirecao().name()).append(' ').append(c.getTimerProducao())
              .append(' ').append(c.getIndiceSaida()).append('\n');

            switch (c.getTipo()) {
                case ESTEIRA: {
                    List<Item> itens = e.itensEsteiras.get(c);
                    if (itens != null) escreverLinhaItens(sb, "B", itens);
                    break;
                }
                case SEPARADOR: {
                    sb.append("M ").append(c.getModoSeparador().name());
                    for (Item.Tipo t : Item.Tipo.values()) sb.append(' ').append(c.getSaidaDoTipo(t));
                    sb.append(' ').append(c.getProximaFaixaRoteador()).append('\n');
                    for (int f = 0; f < 2; f++) escreverLinhaItens(sb, "F " + f, c.getFaixa(f));
                    break;
                }
                default: {
                    if (c.temEntradaSeparada()) escreverLinhaItens(sb, "E", c.getInventarioEntrada());
                    escreverLinhaItens(sb, "S", c.getInventarioSaida());
                    break;
                }
            }
        }
        return sb.toString();
    }

    private void escreverLinhaItens(StringBuilder sb, String prefixo, List<Item> itens) {
        if (itens.isEmpty()) return;
        sb.append(prefixo);
        for (Item i : itens) sb.append(' ').append(itemParaTexto(i));
        sb.append('\n');
    }

    private String itemParaTexto(Item i) {
        return i.getTipo().name() + "," + i.getProgressoTile() + ","
                + (i.getLadoEntrada() == null ? "-" : i.getLadoEntrada().name()) + ","
                + i.getLateral() + "," + i.getFaixaDestino();
    }

    public Estado carregar(File f, Estado padrao) throws IOException {
        Estado e = new Estado();
        e.cameraX = padrao.cameraX;
        e.cameraY = padrao.cameraY;
        e.zoom = padrao.zoom;
        e.selecao = padrao.selecao;
        e.direcao = padrao.direcao;
        Construcao atual = null;

        try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
            String linha = reader.readLine();
            
            if (linha == null || !linha.startsWith("JOGO_FABRICA")) {
                throw new IOException("Arquivo de save inválido ou corrompido");
            }

            int numeroLinha = 1;
            while ((linha = reader.readLine()) != null) {
                linha = linha.trim();
                numeroLinha++;
                
                if (linha.isEmpty()) continue;
                String[] t = linha.split("\\s+");

                try {
                    switch (t[0]) {
                        case "CAM":
                            e.cameraX = Double.parseDouble(t[1]);
                            e.cameraY = Double.parseDouble(t[2]);
                            e.zoom = Math.max(0.15, Math.min(2.5, Double.parseDouble(t[3])));
                            break;
                        case "SEL":
                            e.selecao = t[1].equals("NENHUM") ? null : Construcao.Tipo.valueOf(t[1]);
                            e.direcao = Esteira.Direcao.valueOf(t[2]);
                            break;
                        case "C":
                            atual = new Construcao(Construcao.Tipo.valueOf(t[1]),
                                    Integer.parseInt(t[2]) * tileSize, Integer.parseInt(t[3]) * tileSize,
                                    Esteira.Direcao.valueOf(t[4]));
                            atual.setTimerProducao(Double.parseDouble(t[5]));
                            atual.setIndiceSaida(Integer.parseInt(t[6]));
                            e.construcoes.add(atual);
                            break;
                        case "B": {
                            List<Item> lista = new ArrayList<>();
                            for (int k = 1; k < t.length; k++) lista.add(itemDeTexto(t[k]));
                            e.itensEsteiras.put(atual, lista);
                            break;
                        }
                        case "E":
                            for (int k = 1; k < t.length; k++) atual.getInventarioEntrada().add(itemDeTexto(t[k]));
                            break;
                        case "S":
                            for (int k = 1; k < t.length; k++) atual.getInventarioSaida().add(itemDeTexto(t[k]));
                            break;
                        case "F": {
                            int faixa = Integer.parseInt(t[1]);
                            for (int k = 2; k < t.length; k++) atual.getFaixa(faixa).add(itemDeTexto(t[k]));
                            break;
                        }
                        case "M": {
                            atual.setModoSeparador(Construcao.ModoSeparador.valueOf(t[1]));
                            Item.Tipo[] tipos = Item.Tipo.values();
                            for (int k = 0; k < tipos.length; k++) {
                                atual.setSaidaDoTipo(tipos[k], Integer.parseInt(t[2 + k]) == 0 ? 0 : 1);
                            }
                            atual.setProximaFaixaRoteador(Integer.parseInt(t[2 + tipos.length]));
                            break;
                        }
                    }
                } catch (RuntimeException ex) {
                    throw new IOException("Erro de leitura na linha " + numeroLinha);
                }
            }
        }

        validar(e.construcoes);
        return e;
    }

    private Item itemDeTexto(String s) {
        String[] p = s.split(",");
        Item item = new Item(Item.Tipo.valueOf(p[0]));
        item.setProgressoTile(Double.parseDouble(p[1]));
        item.setLadoEntrada(p[2].equals("-") ? null : Esteira.Direcao.valueOf(p[2]));
        item.setLateral(Double.parseDouble(p[3]));
        item.setFaixaDestino(Integer.parseInt(p[4]));
        return item;
    }

    private void validar(List<Construcao> construcoes) throws IOException {
        Set<String> usados = new HashSet<>();
        for (Construcao c : construcoes) {
            int col = colDe(c);
            int row = rowDe(c);
            if (col < 0 || row < 0 || col + c.getLargura() > mapaLargura || row + c.getAltura() > mapaAltura) {
                throw new IOException("Construção encontrada fora dos limites do mapa");
            }
            for (int i = 0; i < c.getLargura(); i++) {
                for (int j = 0; j < c.getAltura(); j++) {
                    String chave = (col + i) + "," + (row + j);
                    if (!usados.add(chave)) {
                        throw new IOException("Construções sobrepostas encontradas no save");
                    }
                }
            }
        }
    }

    private int colDe(Construcao c) { return (int) Math.round(c.getX() / tileSize); }
    private int rowDe(Construcao c) { return (int) Math.round(c.getY() / tileSize); }
}