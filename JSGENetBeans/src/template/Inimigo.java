package template;

/** os arquivos "Inimigo.java" e "Torre.java" são arquivos da ideia antiga do nosso projeto
* inicialmente iamos fazer um Tower Defense com uma horda de inimigos
* mas tava ficando bem feio e bem chato na real, foi aí que percebi que os as bolinhas
* passando na tela se pareciam muito com os itens passando nas esteiras de joguinhos de
* automação como Factorio, Mindustry, etc
* Resolvi deixar os arquivos aqui caso eu quisesse implementar uma especíe de jogo de defesa
* em que vc tem q automatizar a produção e se defender de uma horda de inimigos
*  */

public class Inimigo {
    
    public enum Tipo {
        NORMAL("Orc", 100, 80.0, 1, 10),
        EMBOSCADOR("Assassino", 60, 140.0, 2, 15),
        XAMA("Xamã", 250, 55.0, 3, 25);

        public final String nome;
        public final int vidaBase;
        public final double velocidadeBase;
        public final int danoBase;
        public final int recompensa;

        Tipo(String nome, int vidaBase, double velocidadeBase, int danoBase, int recompensa) {
            this.nome = nome;
            this.vidaBase = vidaBase;
            this.velocidadeBase = velocidadeBase;
            this.danoBase = danoBase;
            this.recompensa = recompensa;
        }
    }

    private Tipo tipo;
    private int vidaMax;
    private int vidaAtual;
    private double x, y;
    private int noAtualCaminho = 0;

    // Sistema de Efeitos (Habilidade Congelar / Torre de Gelo)
    private double fatorLentidao = 0.0;
    private double tempoLentidao = 0;

    public Inimigo(Tipo tipo, double startX, double startY, double multiplicadorOnda) {
        this.tipo = tipo;
        this.vidaMax = (int) (tipo.vidaBase * multiplicadorOnda); // HP escala com a onda
        this.vidaAtual = this.vidaMax;
        this.x = startX;
        this.y = startY;
    }

    public void atualizarEfeitos(double delta) {
        if (tempoLentidao > 0) {
            tempoLentidao -= delta;
            if (tempoLentidao <= 0) fatorLentidao = 0.0; // Remove o slow quando o tempo acaba
        }
    }

    public void aplicarLentidao(double fator, double duracao) {
        if (fator > this.fatorLentidao) this.fatorLentidao = fator;
        if (duracao > this.tempoLentidao) this.tempoLentidao = duracao;
    }

    public boolean estaLento() { return tempoLentidao > 0; }
    
    public double getVelocidadeAtual() { 
        return tipo.velocidadeBase * (1.0 - fatorLentidao); 
    }

    public void receberDano(int dano) {
        vidaAtual -= dano;
        if (vidaAtual < 0) vidaAtual = 0;
    }

    public boolean estaMorto() { return vidaAtual <= 0; }

    // Getters / Setters
    public Tipo getTipo() { return tipo; }
    public int getVidaMax() { return vidaMax; }
    public int getVidaAtual() { return vidaAtual; }
    public double getX() { return x; }
    public double getY() { return y; }
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public int getNoAtualCaminho() { return noAtualCaminho; }
    public void setNoAtualCaminho(int no) { this.noAtualCaminho = no; }
}