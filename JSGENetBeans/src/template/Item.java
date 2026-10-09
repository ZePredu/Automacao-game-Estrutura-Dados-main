package template;

public class Item {
    public enum Tipo {
        MINERIO_FERRO("Minério de Ferro", 0),
        PLACA_FERRO("Placa de Ferro", 1);

        public final String nome;
        public final int corId;

        Tipo(String nome, int corId) {
            this.nome = nome;
            this.corId = corId;
        }
    }

    private Tipo tipo;
    private double progressoTile = 0.0;

    // Lado do tile da esteira por onde o item entrou (define a trajetória da animação)
    private Esteira.Direcao ladoEntrada;

    // Usados dentro do Separador: posição lateral contínua (0 = faixa 0, 1 = faixa 1)
    // e a faixa de saída para a qual o item está indo.
    private double lateral = 0.0;
    private int faixaDestino = 0;

    public Item(Tipo tipo) {
        this.tipo = tipo;
    }

    public Tipo getTipo() {
        return tipo; 
    }
    public double getProgressoTile() {
        return progressoTile;
    }
    public void setProgressoTile(double progressoTile) {
        this.progressoTile = progressoTile; 
    }
    public Esteira.Direcao getLadoEntrada() { 
        return ladoEntrada; 
    }
    public void setLadoEntrada(Esteira.Direcao ladoEntrada) {
        this.ladoEntrada = ladoEntrada; 
    }
    public double getLateral() {
        return lateral; 
    }
    public void setLateral(double lateral) {
        this.lateral = lateral;
    }
    public int getFaixaDestino() {
        return faixaDestino; 
    }
    public void setFaixaDestino(int faixaDestino) {
        this.faixaDestino = faixaDestino; 
    }
}