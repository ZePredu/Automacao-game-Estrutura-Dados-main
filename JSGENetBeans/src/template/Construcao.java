package template;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Construcao {

    public enum Tipo {
        ESTEIRA("Esteira", 1),
        MINERADORA("Mineradora", 2),
        CONSTRUTORA("Construtora", 3),
        ARMAZEM("Armazém", 4),
        SEPARADOR("Separador", 2); 
        public final String nome;
        public final int tamanho;

        Tipo(String nome, int tamanho) {
            this.nome = nome;
            this.tamanho = tamanho;
        }
    }

    public enum ModoSeparador {
        ROTEADOR("Roteador"),   
        SEPARADOR("Separador"); 

        public final String nome;

        ModoSeparador(String nome) { this.nome = nome; }
    }


    private static boolean horizontal(Esteira.Direcao d) {
        return d == Esteira.Direcao.DIREITA || d == Esteira.Direcao.ESQUERDA;
    }

    public static int larguraDe(Tipo tipo, Esteira.Direcao direcao) {
        if (tipo == Tipo.SEPARADOR) return horizontal(direcao) ? 1 : 2;
        return tipo.tamanho;
    }

    public static int alturaDe(Tipo tipo, Esteira.Direcao direcao) {
        if (tipo == Tipo.SEPARADOR) return horizontal(direcao) ? 2 : 1;
        return tipo.tamanho;
    }


    private Tipo tipo;
    private double x, y;
    private Esteira.Direcao direcao;

    private double timerProducao = 0;
    private final double INTERVALO_PRODUCAO = 2.0;

    private List<Item> inventarioEntrada = new ArrayList<>();
    private List<Item> inventarioSaida = new ArrayList<>();
    private int capacidadeEntrada = 0;
    private int capacidadeSaida = 0;
    private int indiceSaida = 0;

    private final List<LinkedList<Item>> faixas = new ArrayList<>();
    private ModoSeparador modoSeparador = ModoSeparador.ROTEADOR;
    private final int[] saidaPorTipo = new int[Item.Tipo.values().length];
    private int proximaFaixaRoteador = 0;

    private static final int CAPACIDADE_FAIXA = 4;
    private static final double VELOCIDADE_FAIXA = 1.2;
    private static final double VELOCIDADE_LATERAL = 3.0;
    private static final double ESPACAMENTO = 0.25;

    public Construcao(Tipo tipo, double x, double y, Esteira.Direcao direcao) {
        this.tipo = tipo;
        this.x = x;
        this.y = y;
        this.direcao = direcao;

        switch (tipo) {
            case MINERADORA:
                capacidadeEntrada = 0;
                capacidadeSaida = 10;
                break;
            case CONSTRUTORA:
                capacidadeEntrada = 10;
                capacidadeSaida = 10;
                break;
            case ARMAZEM:
                capacidadeEntrada = 40;
                capacidadeSaida = 40;
                inventarioSaida = inventarioEntrada; // estoque compartilhado
                break;
            case SEPARADOR:
                faixas.add(new LinkedList<>());
                faixas.add(new LinkedList<>());
              
                for (Item.Tipo t : Item.Tipo.values()) {
                    saidaPorTipo[t.ordinal()] = t.ordinal() % 2;
                }
                break;
            default:
                break;
        }
    }

    

    public void atualizar(double delta) {
        if (tipo == Tipo.MINERADORA) {
            timerProducao = Math.min(INTERVALO_PRODUCAO, timerProducao + delta);
            if (timerProducao >= INTERVALO_PRODUCAO && inventarioSaida.size() < capacidadeSaida) {
                inventarioSaida.add(new Item(Item.Tipo.MINERIO_FERRO));
                timerProducao = 0;
            }

        } else if (tipo == Tipo.CONSTRUTORA) {
            boolean podeProduzir = !inventarioEntrada.isEmpty() && inventarioSaida.size() < capacidadeSaida;
            if (podeProduzir) {
                timerProducao += delta;
                if (timerProducao >= INTERVALO_PRODUCAO) {
                    inventarioEntrada.remove(inventarioEntrada.size() - 1);
                    inventarioSaida.add(new Item(Item.Tipo.PLACA_FERRO));
                    timerProducao = 0;
                }
            } else if (inventarioEntrada.isEmpty()) {
                timerProducao = 0;
            }
        }
    }
    public Item retirarDoTopoSaida() {
        if (inventarioSaida.isEmpty()) return null;
        return inventarioSaida.remove(inventarioSaida.size() - 1);
    }


    public boolean podeReceber(Item item, Esteira.Direcao direcaoFluxo, int faixa) {
        switch (tipo) {
            case CONSTRUTORA:
                return item.getTipo() == Item.Tipo.MINERIO_FERRO && inventarioEntrada.size() < capacidadeEntrada;
            case ARMAZEM:
                return inventarioEntrada.size() < capacidadeEntrada;
            case SEPARADOR:
                return direcaoFluxo == direcao && faixa >= 0 && faixa < 2 && podeReceberFaixa(faixa);
            default:
                return false;
        }
    }

    public boolean receber(Item item, Esteira.Direcao direcaoFluxo, int faixa) {
        if (!podeReceber(item, direcaoFluxo, faixa)) return false;

        if (tipo == Tipo.SEPARADOR) {
            item.setProgressoTile(0.0);
            item.setLateral(faixa);
            if (modoSeparador == ModoSeparador.ROTEADOR) {
                item.setFaixaDestino(proximaFaixaRoteador);
                proximaFaixaRoteador = 1 - proximaFaixaRoteador;
            } else {
                item.setFaixaDestino(saidaPorTipo[item.getTipo().ordinal()]);
            }
            faixas.get(faixa).addLast(item);
        } else {
            inventarioEntrada.add(item);
        }
        return true;
    }


    private boolean podeReceberFaixa(int faixa) {
        LinkedList<Item> f = faixas.get(faixa);
        if (f.size() >= CAPACIDADE_FAIXA) return false;
        return f.isEmpty() || f.getLast().getProgressoTile() >= ESPACAMENTO;
    }

    public void atualizarSeparador(double delta) {
        for (LinkedList<Item> faixa : faixas) {
            double limite = 1.0;
            for (Item item : faixa) {
                double atual = item.getProgressoTile();
                double novo = Math.min(limite, atual + VELOCIDADE_FAIXA * delta);
                if (novo > atual) item.setProgressoTile(novo);
                limite = item.getProgressoTile() - ESPACAMENTO;

                if (modoSeparador == ModoSeparador.SEPARADOR) {
                    item.setFaixaDestino(saidaPorTipo[item.getTipo().ordinal()]);
                }

                double alvo = item.getFaixaDestino();
                double dif = alvo - item.getLateral();
                double passo = VELOCIDADE_LATERAL * delta;
                if (Math.abs(dif) <= passo) item.setLateral(alvo);
                else item.setLateral(item.getLateral() + Math.signum(dif) * passo);
            }
        }
    }

    public LinkedList<Item> getFaixa(int indice) { return faixas.get(indice); }
    public ModoSeparador getModoSeparador() { return modoSeparador; }
    public void setModoSeparador(ModoSeparador modo) { this.modoSeparador = modo; }
    public int getSaidaDoTipo(Item.Tipo t) { return saidaPorTipo[t.ordinal()]; }
    public void setSaidaDoTipo(Item.Tipo t, int faixa) { saidaPorTipo[t.ordinal()] = faixa; }

    // ---------- Consultas ----------

    public boolean temInventario() { return tipo != Tipo.ESTEIRA; } 

    public boolean temEntradaSeparada() {
        return capacidadeEntrada > 0 && inventarioEntrada != inventarioSaida;
    }

    public double getProgressoProducao() {
        return Math.min(1.0, timerProducao / INTERVALO_PRODUCAO);
    }

    public int getLargura() { 
        return larguraDe(tipo, direcao);
    }
    public int getAltura() {
        return alturaDe(tipo, direcao);
    }

    public List<Item> getInventarioEntrada() {
        return inventarioEntrada; 
    }
    public List<Item> getInventarioSaida() {
        return inventarioSaida; 
    }
    public int getCapacidadeEntrada() {
        return capacidadeEntrada; 
    }
    public int getCapacidadeSaida() {
        return capacidadeSaida;
    }
    public Tipo getTipo() {
        return tipo;
    }
    public double getX() {
        return x; 
    }
    public double getY() {
        return y; 
    }
    public Esteira.Direcao getDirecao() {
        return direcao; 
    }
    public void setDirecao(Esteira.Direcao direcao) { 
        this.direcao = direcao;
    }
    public int getIndiceSaida() {
        return indiceSaida;
    }
    public void setIndiceSaida(int indiceSaida) {
        this.indiceSaida = indiceSaida; 
    }

    public double getTimerProducao() { 
        return timerProducao;
    }
    public void setTimerProducao(double timer) {
        this.timerProducao = Math.max(0, Math.min(INTERVALO_PRODUCAO, timer)); 
    }
    public int getProximaFaixaRoteador() {
        return proximaFaixaRoteador;
    }
    public void setProximaFaixaRoteador(int faixa) {
        this.proximaFaixaRoteador = faixa == 0 ? 0 : 1; 
    }
}