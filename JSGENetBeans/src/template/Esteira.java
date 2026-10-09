package template;

import java.util.LinkedList;
import java.util.List;

public class Esteira {
    public enum Direcao { CIMA, DIREITA, BAIXO, ESQUERDA }

    private double x, y;
    private Direcao direcao;

    private LinkedList<Item> itens = new LinkedList<>();

    private static final int CAPACIDADE_MAX = 5;
    private static final double VELOCIDADE = 1.2;  
    private static final double ESPACAMENTO = 0.2; 

    public Esteira(double x, double y, Direcao direcao) {
        this.x = x;
        this.y = y;
        this.direcao = direcao;
    }

    public static Direcao oposta(Direcao d) {
        switch (d) {
            case CIMA: return Direcao.BAIXO;
            case BAIXO: return Direcao.CIMA;
            case DIREITA: return Direcao.ESQUERDA;
            default: return Direcao.DIREITA;
        }
    }

    
     
    public boolean podeReceber(Direcao ladoEntrada) {
        if (ladoEntrada == direcao)
            return false;
        if (itens.size() >= CAPACIDADE_MAX)
            return false;
        if (!itens.isEmpty() && itens.getLast().getProgressoTile() < ESPACAMENTO)
            return false;
        return true;
    }

    public boolean adicionarItem(Item item, Direcao ladoEntrada) {
        if (!podeReceber(ladoEntrada)) 
            return false;
        item.setProgressoTile(0.0);
        item.setLadoEntrada(ladoEntrada);
        itens.addLast(item);
        return true;
    }

    public void atualizar(double delta, Esteira proximaEsteira, Construcao destinoConstrucao, int faixaDestino) {
        double limite = 1.0;
        for (Item item : itens) {
            double atual = item.getProgressoTile();
            double novo = Math.min(limite, atual + VELOCIDADE * delta);
            if (novo > atual) item.setProgressoTile(novo);
            limite = item.getProgressoTile() - ESPACAMENTO;
        }

        if (!itens.isEmpty()) {
            Item primeiro = itens.getFirst();
            if (primeiro.getProgressoTile() >= 1.0) {
                if (proximaEsteira != null) {
                    if (proximaEsteira.adicionarItem(primeiro, oposta(direcao))) {
                        itens.removeFirst();
                    }
                } else if (destinoConstrucao != null && destinoConstrucao.receber(primeiro, direcao, faixaDestino)) {
                    itens.removeFirst();
                }
            }
        }
    }

    public double[] posicaoItem(Item item) {
        double p = Math.max(0.0, Math.min(1.0, item.getProgressoTile()));
        Direcao lado = item.getLadoEntrada() != null ? item.getLadoEntrada() : oposta(direcao);

        double[] entrada = pontoDaBorda(lado);
        double[] centro = {0.5, 0.5};
        double[] saida = pontoDaBorda(direcao);

        if (p < 0.5) {
            double t = p * 2.0;
            return new double[]{ entrada[0] + (centro[0] - entrada[0]) * t,
                                 entrada[1] + (centro[1] - entrada[1]) * t };
        } else {
            double t = (p - 0.5) * 2.0;
            return new double[]{ centro[0] + (saida[0] - centro[0]) * t,
                                 centro[1] + (saida[1] - centro[1]) * t };
        }
    }

    private static double[] pontoDaBorda(Direcao d) {
        switch (d) {
            case CIMA: return new double[]{0.5, 0.0};
            case DIREITA: return new double[]{1.0, 0.5};
            case BAIXO: return new double[]{0.5, 1.0};
            default: return new double[]{0.0, 0.5};
        }
    }

    public boolean estaCheia() {
        return itens.size() >= CAPACIDADE_MAX; 
    }

    public double getX() {
        return x; 
    }
    public double getY() { 
        return y; 
    }
    public Direcao getDirecao() { 
        return direcao; 
    }
    public void setDirecao(Direcao direcao) {
        this.direcao = direcao; 
    }
    public List<Item> getItens() { 
        return itens; 
    }
}