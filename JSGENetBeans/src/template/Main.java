package template;

import br.com.davidbuzatto.jsge.core.engine.EngineFrame;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.swing.JOptionPane;

/**
 * Alunos: Joao Carlos Silveira & José Pedro Franco
 * 
 * O projeto acabou centralizando muitas responsabilidades nesta classe 
 * devido à complexidade de gerenciar a sincronização gráfica com a lógica de 
 * atualização das esteiras e inventários. Em iterações futuras do projeto, 
 * o ideal seria aplicar padrões de projeto para separar a interface visual 
 * (UI/Renderização) da lógica de domínio (Fábrica/Mundo).
 */
public class Main extends EngineFrame {

    private List<Construcao> construcoes = new ArrayList<>();
    private List<Esteira> esteiras = new ArrayList<>();

    private Map<Ponto, Esteira> mapaEsteiras = new HashMap<>();
    private Map<Ponto, Construcao> ocupacao = new HashMap<>();

    private double cameraX = 0, cameraY = 40;
    private double mouseUltimoX = 0, mouseUltimoY = 0;
    private double zoomScale = 1.0;
    private final int TILE_SIZE = 100;

    private final int MAP_WIDTH = 500;
    private final int MAP_HEIGHT = 500;

    private boolean menuConstrucaoAberto = false;
    private Construcao.Tipo tipoSelecionado = Construcao.Tipo.ESTEIRA;
    private Esteira.Direcao direcaoAtual = Esteira.Direcao.DIREITA;

    private Construcao construcaoInspecionada = null;

    private final Color COR_ARMAZEM = new Color(139, 90, 43);
    private final Color COR_SEPARADOR = new Color(90, 90, 120);

    // Menu de construção
    private final Construcao.Tipo[] TIPOS_MENU = {
        Construcao.Tipo.ESTEIRA, Construcao.Tipo.MINERADORA, Construcao.Tipo.CONSTRUTORA,
        Construcao.Tipo.ARMAZEM, Construcao.Tipo.SEPARADOR
    };
    private final String[] ROTULOS_MENU = {
        "1. Esteira (1x1)", "2. Mineradora (2x2)", "3. Construtora (3x3)",
        "4. Armazém (4x4)", "5. Separador (1x2)"
    };
    private final double MENU_W = 300;
    private final double MENU_H = 335;

    private final int COLUNAS = 10, SLOT = 38, PASSO = 44, PAD = 15, ROTULO = 22;

    private final GerenciadorSave saves = new GerenciadorSave("saves", TILE_SIZE, MAP_WIDTH, MAP_HEIGHT);
    private final double PAUSA_W = 560;
    private final double PAUSA_H = 525;
    private final int LINHAS_VISIVEIS = 7;
    private final double ALTURA_LINHA = 48;

    private boolean pausado = false;
    private List<File> arquivosSalvos = new ArrayList<>();
    private int scrollSalvos = 0;
    private String mensagemPausa = "";

    public Main() {
        super(1600, 900, "Jogo de Automação", 60, true, false, false, false, false, false);
    }

    private void desativarTeclaDeSaidaDaEngine() {
        for (String nome : new String[]{"setExitKey", "setCloseKey"}) {
            try {
                getClass().getMethod(nome, int.class).invoke(this, 0); 
                return;
            } catch (Exception ignorada) {
                
            }
        }
    }

    @Override
    public void create() {
        desativarTeclaDeSaidaDaEngine();

        double centroMundoX = (MAP_WIDTH / 2.0) * TILE_SIZE;
        double centroMundoY = (MAP_HEIGHT / 2.0) * TILE_SIZE;

        cameraX = (getScreenWidth() / 2.0) - centroMundoX;
        cameraY = (getScreenHeight() / 2.0) - centroMundoY;

        adicionarConstrucao(new Construcao(Construcao.Tipo.MINERADORA, centroMundoX - (2*TILE_SIZE), centroMundoY, Esteira.Direcao.DIREITA));
        adicionarConstrucao(new Construcao(Construcao.Tipo.ESTEIRA, centroMundoX, centroMundoY, Esteira.Direcao.DIREITA));
        adicionarConstrucao(new Construcao(Construcao.Tipo.ESTEIRA, centroMundoX + TILE_SIZE, centroMundoY, Esteira.Direcao.DIREITA));
    }


    private Ponto chave(int col, int row) {
        return new Ponto(col, row);
    }

    private int colDe(double mundoX) { 
        return (int) Math.round(mundoX / TILE_SIZE); 
    }
    
    private int rowDe(double mundoY) { 
        return (int) Math.round(mundoY / TILE_SIZE); 
    }

    private int colDoMouse(double mx) { 
        return (int) Math.floor(((mx - cameraX) / zoomScale) / TILE_SIZE); 
    }
    
    private int rowDoMouse(double my) { 
        return (int) Math.floor(((my - cameraY) / zoomScale) / TILE_SIZE); 
    }

    private Esteira esteiraEm(int col, int row) { 
        return mapaEsteiras.get(chave(col, row)); 
    }
    
    private Construcao construcaoEm(int col, int row) { 
        return ocupacao.get(chave(col, row)); 
    }

    private boolean dentro(double mx, double my, double x, double y, double w, double h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }


    private void adicionarConstrucao(Construcao c) {
        construcoes.add(c);

        int col = colDe(c.getX());
        int row = rowDe(c.getY());
        for (int i = 0; i < c.getLargura(); i++) {
            for (int j = 0; j < c.getAltura(); j++) {
                ocupacao.put(chave(col + i, row + j), c);
            }
        }

        if (c.getTipo() == Construcao.Tipo.ESTEIRA) {
            Esteira e = new Esteira(c.getX(), c.getY(), c.getDirecao());
            esteiras.add(e);
            mapaEsteiras.put(chave(col, row), e);
        }
    }

    private void removerConstrucaoNoTile(int col, int row) {
        Construcao c = construcaoEm(col, row);
        
        if (c == null) {
            return;
        }

        int cc = colDe(c.getX());
        int cr = rowDe(c.getY());
        for (int i = 0; i < c.getLargura(); i++) {
            for (int j = 0; j < c.getAltura(); j++) {
                ocupacao.remove(chave(cc + i, cr + j));
            }
        }

        if (c.getTipo() == Construcao.Tipo.ESTEIRA) {
            Esteira e = mapaEsteiras.remove(chave(cc, cr));
            if (e != null) {
                esteiras.remove(e);
            }
        }

        if (c == construcaoInspecionada) {
            construcaoInspecionada = null;
        }
        construcoes.remove(c);
    }

    private void girarEsteira(Esteira e, Esteira.Direcao nova) {
        e.setDirecao(nova);
        Construcao c = construcaoEm(colDe(e.getX()), rowDe(e.getY()));
        if (c != null) {
            c.setDirecao(nova);
        }
    }

    private boolean areaLivre(int col, int row, int largura, int altura) {
        for (int i = 0; i < largura; i++) {
            for (int j = 0; j < altura; j++) {
                if (ocupacao.containsKey(chave(col + i, row + j))) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean dentroDoMapa(int col, int row, int largura, int altura) {
        return col >= 0 && row >= 0 && col + largura <= MAP_WIDTH && row + altura <= MAP_HEIGHT;
    }


    private int faixaDoTile(Construcao c, int col, int row) {
        if (c.getTipo() != Construcao.Tipo.SEPARADOR) {
            return 0;
        }
        boolean horizontal = c.getDirecao() == Esteira.Direcao.DIREITA || c.getDirecao() == Esteira.Direcao.ESQUERDA;
        if (horizontal) {
            return row - rowDe(c.getY());
        } else {
            return col - colDe(c.getX());
        }
    }

    private int[] tileSaidaSeparador(Construcao c, int faixa) {
        int col = colDe(c.getX());
        int row = rowDe(c.getY());
        
        switch (c.getDirecao()) {
            case DIREITA:  
                return new int[]{col + 1, row + faixa};
            case ESQUERDA: 
                return new int[]{col - 1, row + faixa};
            case BAIXO:    
                return new int[]{col + faixa, row + 1};
            default:       
                return new int[]{col + faixa, row - 1};
        }
    }


    private boolean entregarEm(Item item, int col, int row, Esteira.Direcao fluxo, boolean executar) {
        Esteira e = esteiraEm(col, row);
        if (e != null) {
            Esteira.Direcao entrada = Esteira.oposta(fluxo);
            if (!e.podeReceber(entrada)) {
                return false;
            }
            if (executar) {
                e.adicionarItem(item, entrada);
            }
            return true;
        }

        Construcao alvo = construcaoEm(col, row);
        if (alvo != null) {
            int faixa = faixaDoTile(alvo, col, row);
            if (!alvo.podeReceber(item, fluxo, faixa)) {
                return false;
            }
            if (executar) {
                alvo.receber(item, fluxo, faixa);
            }
            return true;
        }
        return false;
    }

    private boolean entregarSaidaSeparador(Construcao c, int faixa, Item item, boolean executar) {
        int[] t = tileSaidaSeparador(c, faixa);
        return entregarEm(item, t[0], t[1], c.getDirecao(), executar);
    }

    private void atualizarSeparador(Construcao c, double delta) {
        c.atualizarSeparador(delta);

        for (int f = 0; f < 2; f++) {
            LinkedList<Item> fila = c.getFaixa(f);
            if (fila.isEmpty()) {
                continue;
            }

            Item frente = fila.getFirst();
            if (frente.getProgressoTile() < 1.0) {
                continue;
            }


            if (c.getModoSeparador() == Construcao.ModoSeparador.ROTEADOR) {
                int pref = frente.getFaixaDestino();
                boolean viavelPrincipal = entregarSaidaSeparador(c, pref, frente, false);
                boolean viavelAlternativa = entregarSaidaSeparador(c, 1 - pref, frente, false);
                
                if (!viavelPrincipal && viavelAlternativa) {
                    frente.setFaixaDestino(1 - pref);
                }
            }

            if (Math.abs(frente.getLateral() - frente.getFaixaDestino()) < 0.01) {
                if (entregarSaidaSeparador(c, frente.getFaixaDestino(), frente, true)) {
                    fila.removeFirst();
                }
            }
        }
    }


    private double[] retanguloJanela(Construcao c) {
        double w, h;
        if (c.getTipo() == Construcao.Tipo.SEPARADOR) {
            w = 380;
            h = 250;
        } else {
            boolean separado = c.temEntradaSeparada();
            
            int linhasEntrada = 0;
            if (separado) {
                linhasEntrada = (int) Math.ceil(c.getCapacidadeEntrada() / (double) COLUNAS);
            }
            
            int linhasSaida = (int) Math.ceil(c.getCapacidadeSaida() / (double) COLUNAS);
            w = PAD * 2 + COLUNAS * PASSO - (PASSO - SLOT);
            
            double alturaExtra = 0;
            if (separado) {
                alturaExtra = ROTULO + linhasEntrada * PASSO + 6;
            }
            
            h = 30 + 10 + alturaExtra + ROTULO + linhasSaida * PASSO + 48; // legenda + dica da pilha
        }
        return new double[]{ getScreenWidth() / 2.0 - w / 2, getScreenHeight() / 2.0 - h / 2, w, h };
    }

    private boolean mouseNaJanela(double mx, double my) {
        if (construcaoInspecionada == null) {
            return false;
        }
        double[] r = retanguloJanela(construcaoInspecionada);
        return dentro(mx, my, r[0], r[1], r[2], r[3]);
    }

    @Override
    public void update(double delta) {
        double mx = getMouseX();
        double my = getMouseY();
        boolean cliqueEsquerdo = isMouseButtonPressed(MOUSE_BUTTON_LEFT);
        boolean segurandoEsquerdo = isMouseButtonDown(MOUSE_BUTTON_LEFT);
        boolean segurandoDireito = isMouseButtonDown(MOUSE_BUTTON_RIGHT);

        if (isKeyPressed(KEY_P)) {
            alternarPausa();
        }

        if (pausado) {
            if (isKeyPressed(KEY_ESCAPE)) {
                pausado = false;
            } else {
                atualizarMenuPausa(mx, my, cliqueEsquerdo);
            }
            return;
        }

        atualizarCamera(mx, my);

        if (isKeyPressed(KEY_C)) {
            menuConstrucaoAberto = !menuConstrucaoAberto;
        }

        if (isKeyPressed(KEY_ESCAPE)) {
            if (menuConstrucaoAberto) {
                menuConstrucaoAberto = false;
            } else if (construcaoInspecionada != null) {
                construcaoInspecionada = null;
            }
        }

        if (isKeyPressed(KEY_R)) {
            switch (direcaoAtual) {
                case CIMA: 
                    direcaoAtual = Esteira.Direcao.DIREITA; 
                    break;
                case DIREITA: 
                    direcaoAtual = Esteira.Direcao.BAIXO; 
                    break;
                case BAIXO: 
                    direcaoAtual = Esteira.Direcao.ESQUERDA; 
                    break;
                case ESQUERDA: 
                    direcaoAtual = Esteira.Direcao.CIMA; 
                    break;
            }
        }

        if (isKeyPressed(KEY_Q) && !menuConstrucaoAberto) {
            tipoSelecionado = null;

            Construcao alvo = null;
            if (!mouseEmUI(getScreenHeight()) && !mouseNaJanela(mx, my)) {
                Construcao sob = construcaoEm(colDoMouse(mx), rowDoMouse(my));
                if (sob != null && sob.temInventario()) {
                    alvo = sob;
                }
            }

            if (alvo == construcaoInspecionada) {
                construcaoInspecionada = null;
            } else {
                construcaoInspecionada = alvo;
            }
        }

        if (menuConstrucaoAberto && cliqueEsquerdo) {
            verificarCliquesMenu(mx, my);
            return;
        }

        boolean naJanela = mouseNaJanela(mx, my);

        if (naJanela && cliqueEsquerdo && construcaoInspecionada.getTipo() == Construcao.Tipo.SEPARADOR) {
            cliqueJanelaSeparador(construcaoInspecionada, mx, my);
        }

        if (!mouseEmUI(getScreenHeight()) && !menuConstrucaoAberto && !naJanela) {
            int col = colDoMouse(mx);
            int row = rowDoMouse(my);

            if (segurandoDireito) {
                removerConstrucaoNoTile(col, row);
            }

            if (segurandoEsquerdo && tipoSelecionado != null) {
                int larg = Construcao.larguraDe(tipoSelecionado, direcaoAtual);
                int alt = Construcao.alturaDe(tipoSelecionado, direcaoAtual);

                if (dentroDoMapa(col, row, larg, alt)) {
                    Esteira existente = esteiraEm(col, row);
                    if (tipoSelecionado == Construcao.Tipo.ESTEIRA && existente != null) {
                   
                        if (existente.getDirecao() != direcaoAtual) {
                            girarEsteira(existente, direcaoAtual);
                        }
                    } else if (areaLivre(col, row, larg, alt)) {
                        adicionarConstrucao(new Construcao(tipoSelecionado, col * TILE_SIZE, row * TILE_SIZE, direcaoAtual));
                    }
                }
            }
        }

        for (Construcao c : construcoes) {
            if (c.getTipo() == Construcao.Tipo.ESTEIRA) {
                continue;
            }

            if (c.getTipo() == Construcao.Tipo.SEPARADOR) {
                atualizarSeparador(c, delta);
            } else {
                c.atualizar(delta);
                despachar(c);
            }
        }


        for (Esteira est : esteiras) {
            int col = colDe(est.getX());
            int row = rowDe(est.getY());
            switch (est.getDirecao()) {
                case DIREITA: 
                    col++; 
                    break;
                case ESQUERDA: 
                    col--; 
                    break;
                case CIMA: 
                    row--; 
                    break;
                case BAIXO: 
                    row++; 
                    break;
            }
            Esteira proxima = esteiraEm(col, row);
            Construcao destino = null;
            if (proxima == null) {
                destino = construcaoEm(col, row);
            }
            
            int faixa = 0;
            if (destino != null) {
                faixa = faixaDoTile(destino, col, row);
            }
            
            est.atualizar(delta, proxima, destino, faixa);
        }
    }

    private void despachar(Construcao c) {
        if (c.getInventarioSaida().isEmpty()) {
            return;
        }

        int w = c.getLargura();
        int h = c.getAltura();
        int col = colDe(c.getX());
        int row = rowDe(c.getY());
        int total = 2 * (w + h);
        int inicio = c.getIndiceSaida() % total;

        for (int k = 0; k < total; k++) {
            int idx = (inicio + k) % total;

            int ec, er;
            Esteira.Direcao entrada; 
            if (idx < h) {                       
                ec = col + w; 
                er = row + idx; 
                entrada = Esteira.Direcao.ESQUERDA;
            } else if (idx < h + w) {            
                ec = col + (idx - h); 
                er = row + h; 
                entrada = Esteira.Direcao.CIMA;
            } else if (idx < 2 * h + w) {        
                ec = col - 1; 
                er = row + (idx - h - w); 
                entrada = Esteira.Direcao.DIREITA;
            } else {                             
                ec = col + (idx - 2 * h - w); 
                er = row - 1; 
                entrada = Esteira.Direcao.BAIXO;
            }

            Esteira e = esteiraEm(ec, er);
            if (e != null && e.podeReceber(entrada)) {
                e.adicionarItem(c.retirarDoTopoSaida(), entrada);
                c.setIndiceSaida((idx + 1) % total);
                return;
            }
        }
    }

    private void verificarCliquesMenu(double mx, double my) {
        double menuX = getScreenWidth() / 2.0 - MENU_W / 2;
        double menuY = getScreenHeight() / 2.0 - MENU_H / 2;

        for (int i = 0; i < TIPOS_MENU.length; i++) {
            double by = menuY + 50 + i * 55;
            if (dentro(mx, my, menuX + 20, by, 260, 40)) {
                tipoSelecionado = TIPOS_MENU[i];
                menuConstrucaoAberto = false;
            }
        }
    }


    private void cliqueJanelaSeparador(Construcao c, double mx, double my) {
        double[] r = retanguloJanela(c);
        double wx = r[0];
        double wy = r[1];

        if (dentro(mx, my, wx + 20, wy + 45, 165, 34)) {
            c.setModoSeparador(Construcao.ModoSeparador.ROTEADOR);
        }
        if (dentro(mx, my, wx + 195, wy + 45, 165, 34)) {
            c.setModoSeparador(Construcao.ModoSeparador.SEPARADOR);
        }

        if (c.getModoSeparador() == Construcao.ModoSeparador.SEPARADOR) {
            Item.Tipo[] tipos = Item.Tipo.values();
            for (int i = 0; i < tipos.length; i++) {
                double rowY = wy + 110 + i * 60;
                if (dentro(mx, my, wx + 20, rowY + 22, 165, 30)) {
                    c.setSaidaDoTipo(tipos[i], 0);
                }
                if (dentro(mx, my, wx + 195, rowY + 22, 165, 30)) {
                    c.setSaidaDoTipo(tipos[i], 1);
                }
            }
        }
    }

    private void atualizarCamera(double mx, double my) {
        double wheel = getMouseWheelMove();
        if (wheel != 0) {
            double zoomAntigo = zoomScale;
            if (wheel < 0) {
                zoomScale -= 0.1;
            } else {
                zoomScale += 0.1;
            }
            zoomScale = Math.max(0.15, Math.min(2.5, zoomScale));
            cameraX = mx - (mx - cameraX) * (zoomScale / zoomAntigo);
            cameraY = my - (my - cameraY) * (zoomScale / zoomAntigo);
        }
        
        if (isMouseButtonPressed(MOUSE_BUTTON_MIDDLE)) { 
            mouseUltimoX = mx; 
            mouseUltimoY = my; 
        }
        
        if (isMouseButtonDown(MOUSE_BUTTON_MIDDLE)) {
            cameraX += mx - mouseUltimoX;
            cameraY += my - mouseUltimoY;
            mouseUltimoX = mx;
            mouseUltimoY = my;
        }
    }


    private Color corTipo(Item.Tipo t) {
        if (t == Item.Tipo.PLACA_FERRO) {
            return WHITE;
        } else {
            return YELLOW;
        }
    }

    private Color corItem(Item item) {
        return corTipo(item.getTipo());
    }

    @Override
    public void draw() {
        clearBackground(DARKGRAY);
        double z = zoomScale;

        int screenW = getScreenWidth();
        int screenH = getScreenHeight();

        int startCol = (int) Math.max(0, -cameraX / (TILE_SIZE * z));
        int startRow = (int) Math.max(0, (-cameraY + 35) / (TILE_SIZE * z));
        int endCol = (int) Math.min(MAP_WIDTH, startCol + (screenW / (TILE_SIZE * z)) + 2);
        int endRow = (int) Math.min(MAP_HEIGHT, startRow + (screenH / (TILE_SIZE * z)) + 2);

        for (int c = startCol; c < endCol; c++) {
            for (int l = startRow; l < endRow; l++) {
                double posX = c * TILE_SIZE * z + cameraX;
                double posY = l * TILE_SIZE * z + cameraY;
                fillRectangle(posX, posY, TILE_SIZE * z, TILE_SIZE * z, GRAY);
                drawRectangle(posX, posY, TILE_SIZE * z, TILE_SIZE * z, DARKGRAY);
            }
        }

        for (Construcao c : construcoes) {
            if (c.getTipo() == Construcao.Tipo.ESTEIRA) {
                continue;
            }

            double cX = c.getX() * z + cameraX;
            double cY = c.getY() * z + cameraY;
            double largTotal = c.getLargura() * TILE_SIZE * z;
            double altTotal = c.getAltura() * TILE_SIZE * z;

            if (cX + largTotal < 0 || cX > screenW || cY + altTotal < 35 || cY > screenH) {
                continue;
            }

            double barraW = largTotal - 20;
            double barraH = 10;
            double barraX = cX + 10;
            double barraY = cY + altTotal - 20;

            switch (c.getTipo()) {
                case MINERADORA:
                    fillRectangle(cX, cY, largTotal, altTotal, BLUE);
                    drawRectangle(cX, cY, largTotal, altTotal, BLACK);
                    drawText("Mineradora", cX + 10, cY + 10, 14, WHITE);
                    drawText("Saída: " + c.getInventarioSaida().size() + "/" + c.getCapacidadeSaida(), cX + 10, cY + 28, 12, LIGHTGRAY);
                    desenharBarra(barraX, barraY, barraW, barraH, c.getProgressoProducao());
                    break;

                case CONSTRUTORA:
                    fillRectangle(cX, cY, largTotal, altTotal, ORANGE);
                    drawRectangle(cX, cY, largTotal, altTotal, BLACK);
                    drawText("Construtora", cX + 10, cY + 10, 14, BLACK);
                    drawText("Entrada: " + c.getInventarioEntrada().size() + "/" + c.getCapacidadeEntrada(), cX + 10, cY + 28, 12, BLACK);
                    drawText("Saída: " + c.getInventarioSaida().size() + "/" + c.getCapacidadeSaida(), cX + 10, cY + 44, 12, BLACK);
                    desenharBarra(barraX, barraY, barraW, barraH, c.getProgressoProducao());
                    break;

                case ARMAZEM:
                    fillRectangle(cX, cY, largTotal, altTotal, COR_ARMAZEM);
                    drawRectangle(cX, cY, largTotal, altTotal, BLACK);
                    drawText("Armazém", cX + 10, cY + 10, 14, WHITE);
                    drawText("Estoque: " + c.getInventarioEntrada().size() + "/" + c.getCapacidadeEntrada(), cX + 10, cY + 28, 12, LIGHTGRAY);
                    break;

                case SEPARADOR:
                    desenharSeparador(c, cX, cY, z);
                    break;

                default:
                    break;
            }


            if (c == construcaoInspecionada) {
                drawRectangle(cX - 2, cY - 2, largTotal + 4, altTotal + 4, WHITE);
                drawRectangle(cX - 3, cY - 3, largTotal + 6, altTotal + 6, WHITE);
            }
        }

        for (Esteira est : esteiras) {
            double eX = est.getX() * z + cameraX;
            double eY = est.getY() * z + cameraY;
            double s = TILE_SIZE * z;

            if (eX + s < 0 || eX > screenW || eY + s < 35 || eY > screenH) {
                continue;
            }

            fillRectangle(eX, eY, s, s, LIGHTGRAY);
            drawRectangle(eX, eY, s, s, BLACK);
            desenharSetaEsteira(eX, eY, s, est.getDirecao(), DARKGRAY);

            for (Item item : est.getItens()) {
                double[] p = est.posicaoItem(item);
                double iX = eX + p[0] * s;
                double iY = eY + p[1] * s;

                fillCircle(iX, iY, 10 * z, corItem(item));
                drawCircle(iX, iY, 10 * z, BLACK);
            }
        }

        if (tipoSelecionado != null && !pausado && !menuConstrucaoAberto && !mouseEmUI(screenH) && !mouseNaJanela(getMouseX(), getMouseY())) {
            int col = colDoMouse(getMouseX());
            int row = rowDoMouse(getMouseY());
            int larg = Construcao.larguraDe(tipoSelecionado, direcaoAtual);
            int alt = Construcao.alturaDe(tipoSelecionado, direcaoAtual);

            if (dentroDoMapa(col, row, larg, alt)) {
                double pX = col * TILE_SIZE * z + cameraX;
                double pY = row * TILE_SIZE * z + cameraY;
                double s = TILE_SIZE * z;
                boolean livre = areaLivre(col, row, larg, alt);
                boolean podeGirar = tipoSelecionado == Construcao.Tipo.ESTEIRA && esteiraEm(col, row) != null;

                Color corPreview;
                if (livre || podeGirar) {
                    corPreview = LIGHTGRAY;
                } else {
                    corPreview = RED;
                }
                
                fillRectangle(pX, pY, larg * s, alt * s, corPreview);
                drawRectangle(pX, pY, larg * s, alt * s, WHITE);

                if (tipoSelecionado == Construcao.Tipo.ESTEIRA) {
                    desenharSetaEsteira(pX, pY, s, direcaoAtual, GRAY);
                } else if (tipoSelecionado == Construcao.Tipo.SEPARADOR) {
                    boolean horizontal = direcaoAtual == Esteira.Direcao.DIREITA || direcaoAtual == Esteira.Direcao.ESQUERDA;
                    desenharSetaEsteira(pX, pY, s, direcaoAtual, GRAY);
                    
                    double coordXAuxiliar = pX;
                    double coordYAuxiliar = pY;
                    if (horizontal) {
                        coordYAuxiliar += s;
                    } else {
                        coordXAuxiliar += s;
                    }
                    desenharSetaEsteira(coordXAuxiliar, coordYAuxiliar, s, direcaoAtual, GRAY);
                }
            }
        }

        if (construcaoInspecionada != null) {
            if (construcaoInspecionada.getTipo() == Construcao.Tipo.SEPARADOR) {
                desenharJanelaSeparador(construcaoInspecionada);
            } else {
                desenharJanelaInventario(construcaoInspecionada);
            }
        }

        if (menuConstrucaoAberto) {
            double menuX = screenW / 2.0 - MENU_W / 2;
            double menuY = screenH / 2.0 - MENU_H / 2;

            fillRectangle(menuX, menuY, MENU_W, MENU_H, DARKGRAY);
            drawRectangle(menuX, menuY, MENU_W, MENU_H, BLACK);
            fillRectangle(menuX, menuY, MENU_W, 35, GRAY);
            drawText("Menu de Construção [C]", menuX + 15, menuY + 10, 16, WHITE);

            for (int i = 0; i < TIPOS_MENU.length; i++) {
                double by = menuY + 50 + i * 55;
                
                Color corBotao;
                if (tipoSelecionado == TIPOS_MENU[i]) {
                    corBotao = GREEN;
                } else {
                    corBotao = LIGHTGRAY;
                }
                
                fillRectangle(menuX + 20, by, 260, 40, corBotao);
                drawRectangle(menuX + 20, by, 260, 40, BLACK);
                drawText(ROTULOS_MENU[i], menuX + 35, by + 12, 14, BLACK);
            }
        }

        fillRectangle(0, 0, screenW, 35, BLACK);
        
        String selecao;
        if (tipoSelecionado == null) {
            selecao = "NENHUM";
        } else {
            selecao = tipoSelecionado.name();
        }
        
        drawText("SELECIONADO: " + selecao + " | DIREÇÃO: " + direcaoAtual.name()
                + " | [C] Menu | [R] Girar | [Q] Desselecionar / Inspecionar | [P] Pausa/Salvar | Esq: Construir | Dir: Remover",
                20, 10, 14, WHITE);

        if (pausado) {
            desenharMenuPausa(screenW, screenH);
        }
    }

    private void desenharBarra(double x, double y, double w, double h, double progresso) {
        fillRectangle(x, y, w, h, BLACK);
        fillRectangle(x, y, w * progresso, h, GREEN);
        drawRectangle(x, y, w, h, WHITE);
    }


    private double[] vetorDirecao(Esteira.Direcao d) {
        switch (d) {
            case DIREITA:  
                return new double[]{1, 0};
            case ESQUERDA: 
                return new double[]{-1, 0};
            case CIMA:     
                return new double[]{0, -1};
            default:       
                return new double[]{0, 1};
        }
    }

    private void desenharSeparador(Construcao c, double cX, double cY, double z) {
        double s = TILE_SIZE * z;
        Esteira.Direcao dir = c.getDirecao();
        boolean horizontal = dir == Esteira.Direcao.DIREITA || dir == Esteira.Direcao.ESQUERDA;
        double[] f = vetorDirecao(dir);

        fillRectangle(cX, cY, c.getLargura() * s, c.getAltura() * s, COR_SEPARADOR);

        for (int faixa = 0; faixa < 2; faixa++) {
            double lx = cX;
            double ly = cY;
            if (horizontal) {
                ly += faixa * s;
            } else {
                lx += faixa * s;
            }
            
            fillRectangle(lx + 4, ly + 4, s - 8, s - 8, LIGHTGRAY);
            desenharSetaEsteira(lx, ly, s, dir, DARKGRAY);
            drawRectangle(lx, ly, s, s, BLACK);
        }

        drawText(c.getModoSeparador().nome, cX + 6, cY + 4, 11, WHITE);


        if (c.getModoSeparador() == Construcao.ModoSeparador.SEPARADOR) {
            Item.Tipo[] tipos = Item.Tipo.values();
            for (int i = 0; i < tipos.length; i++) {
                int faixa = c.getSaidaDoTipo(tipos[i]);
                
                double lx = cX;
                double ly = cY;
                if (horizontal) {
                    ly += faixa * s;
                } else {
                    lx += faixa * s;
                }
                
                double desloc = (i % 2 == 0 ? -0.2 : 0.2) * s;
                double mx = lx + s / 2 + f[0] * 0.36 * s + (-f[1]) * desloc;
                double my = ly + s / 2 + f[1] * 0.36 * s + f[0] * desloc;
                fillCircle(mx, my, 6 * z, corTipo(tipos[i]));
                drawCircle(mx, my, 6 * z, BLACK);
            }
        }


        for (int faixa = 0; faixa < 2; faixa++) {
            for (Item item : c.getFaixa(faixa)) {
                double p = Math.max(0, Math.min(1, item.getProgressoTile()));
                double cruzado = item.getLateral() + 0.5; 
                double iX, iY;
                
                switch (dir) {
                    case DIREITA:  
                        iX = cX + p * s;       
                        iY = cY + cruzado * s; 
                        break;
                    case ESQUERDA: 
                        iX = cX + (1 - p) * s; 
                        iY = cY + cruzado * s; 
                        break;
                    case BAIXO:    
                        iX = cX + cruzado * s; 
                        iY = cY + p * s;       
                        break;
                    default:       
                        iX = cX + cruzado * s; 
                        iY = cY + (1 - p) * s; 
                        break;
                }
                
                fillCircle(iX, iY, 10 * z, corItem(item));
                drawCircle(iX, iY, 10 * z, BLACK);
            }
        }
    }

    private String rotuloFaixa(Construcao c, int faixa) {
        boolean horizontal = c.getDirecao() == Esteira.Direcao.DIREITA || c.getDirecao() == Esteira.Direcao.ESQUERDA;
        if (horizontal) {
            if (faixa == 0) {
                return "Cima";
            } else {
                return "Baixo";
            }
        } else {
            if (faixa == 0) {
                return "Esquerda";
            } else {
                return "Direita";
            }
        }
    }

    private void desenharBotao(double x, double y, double w, double h, String texto, boolean ativo) {
        Color corDoFundo;
        if (ativo) {
            corDoFundo = GREEN;
        } else {
            corDoFundo = LIGHTGRAY;
        }
        
        fillRectangle(x, y, w, h, corDoFundo);
        drawRectangle(x, y, w, h, BLACK);
        drawText(texto, x + 12, y + h / 2 - 7, 14, BLACK);
    }

    private void desenharJanelaSeparador(Construcao c) {
        double[] r = retanguloJanela(c);
        double wx = r[0];
        double wy = r[1];
        double w = r[2];
        double h = r[3];

        fillRectangle(wx, wy, w, h, DARKGRAY);
        drawRectangle(wx, wy, w, h, BLACK);
        fillRectangle(wx, wy, w, 30, GRAY);
        drawText("Separador (1x2)   [Q/ESC] fechar", wx + 10, wy + 8, 14, WHITE);

        boolean modoSep = c.getModoSeparador() == Construcao.ModoSeparador.SEPARADOR;
        desenharBotao(wx + 20, wy + 45, 165, 34, "Roteador", !modoSep);
        desenharBotao(wx + 195, wy + 45, 165, 34, "Separador de itens", modoSep);

        if (!modoSep) {
            drawText("Alterna os itens entre as duas saídas.", wx + 20, wy + 105, 13, WHITE);
            drawText("Se uma saída estiver bloqueada, usa a outra.", wx + 20, wy + 128, 13, LIGHTGRAY);
            drawText("Entradas: pelas duas esteiras de trás.", wx + 20, wy + 165, 12, LIGHTGRAY);
        } else {
            drawText("Lado de saída de cada item (independe da entrada):", wx + 20, wy + 92, 12, LIGHTGRAY);

            Item.Tipo[] tipos = Item.Tipo.values();
            for (int i = 0; i < tipos.length; i++) {
                double rowY = wy + 110 + i * 60;
                fillCircle(wx + 28, rowY + 8, 7, corTipo(tipos[i]));
                drawCircle(wx + 28, rowY + 8, 7, BLACK);
                drawText(tipos[i].nome, wx + 42, rowY + 1, 14, WHITE);

                int escolhida = c.getSaidaDoTipo(tipos[i]);
                desenharBotao(wx + 20, rowY + 22, 165, 30, rotuloFaixa(c, 0), escolhida == 0);
                desenharBotao(wx + 195, rowY + 22, 165, 30, rotuloFaixa(c, 1), escolhida == 1);
            }
        }
    }


    private void desenharJanelaInventario(Construcao c) {
        double[] r = retanguloJanela(c);
        double wx = r[0];
        double wy = r[1];
        double w = r[2];
        double h = r[3];

        boolean separado = c.temEntradaSeparada();
        int linhasEntrada = 0;
        if (separado) {
            linhasEntrada = (int) Math.ceil(c.getCapacidadeEntrada() / (double) COLUNAS);
        }
        
        int linhasSaida = (int) Math.ceil(c.getCapacidadeSaida() / (double) COLUNAS);

        fillRectangle(wx, wy, w, h, DARKGRAY);
        drawRectangle(wx, wy, w, h, BLACK);
        fillRectangle(wx, wy, w, 30, GRAY);
        drawText("Inventário: " + c.getTipo().nome + "   [Q/ESC] fechar", wx + 10, wy + 8, 14, WHITE);

        double y = wy + 40;

        if (separado) {
            desenharSlots("Entrada", c.getInventarioEntrada(), c.getCapacidadeEntrada(), wx + PAD, y);
            y += ROTULO + linhasEntrada * PASSO + 6;
        }

        String rotuloSaida;
        if (c.getTipo() == Construcao.Tipo.ARMAZEM) {
            rotuloSaida = "Estoque";
        } else {
            rotuloSaida = "Saída";
        }
        
        desenharSlots(rotuloSaida, c.getInventarioSaida(), c.getCapacidadeSaida(), wx + PAD, y);
        y += ROTULO + linhasSaida * PASSO;

        // Legenda
        fillCircle(wx + PAD + 8, y + 12, 7, YELLOW);
        drawCircle(wx + PAD + 8, y + 12, 7, BLACK);
        drawText(Item.Tipo.MINERIO_FERRO.nome, wx + PAD + 22, y + 6, 12, WHITE);
        fillCircle(wx + PAD + 178, y + 12, 7, WHITE);
        drawCircle(wx + PAD + 178, y + 12, 7, BLACK);
        drawText(Item.Tipo.PLACA_FERRO.nome, wx + PAD + 192, y + 6, 12, WHITE);
        drawText("Pilha (LIFO): borda verde = topo (próximo a sair)", wx + PAD, y + 26, 12, LIGHTGRAY);
    }

    private void desenharSlots(String rotulo, List<Item> itens, int capacidade, double x, double y) {
        drawText(rotulo + " (" + itens.size() + "/" + capacidade + ")", x, y, 13, WHITE);
        double base = y + 22;

        for (int i = 0; i < capacidade; i++) {
            double sx = x + (i % COLUNAS) * PASSO;
            double sy = base + (i / COLUNAS) * PASSO;

            fillRectangle(sx, sy, SLOT, SLOT, GRAY);
            drawRectangle(sx, sy, SLOT, SLOT, BLACK);

            if (i < itens.size()) {
                fillCircle(sx + SLOT / 2.0, sy + SLOT / 2.0, 12, corItem(itens.get(i)));
                drawCircle(sx + SLOT / 2.0, sy + SLOT / 2.0, 12, BLACK);
            }

            if (i == itens.size() - 1) {
                drawRectangle(sx - 1, sy - 1, SLOT + 2, SLOT + 2, GREEN);
                drawRectangle(sx - 2, sy - 2, SLOT + 4, SLOT + 4, GREEN);
            }
        }
    }


    private void alternarPausa() {
        pausado = !pausado;
        if (pausado) {
            mensagemPausa = "";
            scrollSalvos = 0;
            atualizarListaSaves();
        }
    }

    private void atualizarListaSaves() {
        arquivosSalvos = saves.listar();
        limitarScroll();
    }

    private void limitarScroll() {
        int max = Math.max(0, arquivosSalvos.size() - LINHAS_VISIVEIS);
        scrollSalvos = Math.max(0, Math.min(max, scrollSalvos));
    }

    private boolean confirmar(String mensagem) {
        return JOptionPane.showConfirmDialog(null, mensagem, "Confirmação", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    private void atualizarMenuPausa(double mx, double my, boolean clique) {
        double wx = getScreenWidth() / 2.0 - PAUSA_W / 2;
        double wy = getScreenHeight() / 2.0 - PAUSA_H / 2;
        double listaY = wy + 112;

        double roda = getMouseWheelMove();
        if (roda != 0 && dentro(mx, my, wx + 20, listaY, PAUSA_W - 40, LINHAS_VISIVEIS * ALTURA_LINHA)) {
            if (roda < 0) {
                scrollSalvos -= 1;
            } else {
                scrollSalvos += 1;
            }
            limitarScroll();
        }

        if (!clique) {
            return;
        }

        if (dentro(mx, my, wx + 20, wy + 45, 160, 34)) {
            pausado = false;
            return;
        }
        
        if (dentro(mx, my, wx + 200, wy + 45, 200, 34)) {
            salvarNovo();
            return;
        }

        if (dentro(mx, my, wx + 20, wy + 456, 100, 30)) {
            scrollSalvos -= LINHAS_VISIVEIS;
            limitarScroll();
            return;
        }
        
        if (dentro(mx, my, wx + 130, wy + 456, 100, 30)) {
            scrollSalvos += LINHAS_VISIVEIS;
            limitarScroll();
            return;
        }

        for (int i = 0; i < LINHAS_VISIVEIS; i++) {
            int idx = scrollSalvos + i;
            
            if (idx >= arquivosSalvos.size()) {
                break;
            }

            File f = arquivosSalvos.get(idx);
            double by = listaY + i * ALTURA_LINHA + 8;

            if (dentro(mx, my, wx + 270, by, 85, 30)) {            
                carregarSave(f);
                return;
            }
            
            if (dentro(mx, my, wx + 360, by, 95, 30)) {           
                if (confirmar("Sobrescrever \"" + saves.nomeDe(f) + "\" com o estado atual?")) {
                    salvarEm(f);
                }
                return;
            }
            
            if (dentro(mx, my, wx + 460, by, 80, 30)) {            
                if (confirmar("Excluir o estado \"" + saves.nomeDe(f) + "\"?")) {
                    boolean excluiu = saves.excluir(f);
                    if (excluiu) {
                        mensagemPausa = "Excluído: " + saves.nomeDe(f);
                    } else {
                        mensagemPausa = "Não foi possível excluir o arquivo.";
                    }
                    atualizarListaSaves();
                }
                return;
            }
        }
    }

    private void salvarNovo() {
        Object resposta = JOptionPane.showInputDialog(null, "Nome do estado salvo:", "Salvar jogo",
                JOptionPane.QUESTION_MESSAGE, null, null, saves.sugestaoDeNome());
                
        if (resposta == null) {
            return; 
        }

        File f = saves.arquivoPara(resposta.toString());
        if (f.exists() && !confirmar("Já existe um estado chamado \"" + saves.nomeDe(f) + "\". Sobrescrever?")) {
            return;
        }
        
        salvarEm(f);
    }

    private void salvarEm(File f) {
        try {
            saves.salvar(f, estadoAtual());
            mensagemPausa = "Salvo: " + saves.nomeDe(f);
        } catch (IOException ex) {
            mensagemPausa = "Erro ao salvar: " + ex.getMessage();
        }
        atualizarListaSaves();
    }

    private void carregarSave(File f) {
        try {
            aplicarEstado(saves.carregar(f, estadoAtual()));
            mensagemPausa = "Carregado: " + saves.nomeDe(f);
            pausado = false;
        } catch (IOException | RuntimeException ex) {
            mensagemPausa = "Erro ao carregar: " + ex.getMessage();
        }
    }

    private GerenciadorSave.Estado estadoAtual() {
        GerenciadorSave.Estado e = new GerenciadorSave.Estado();
        e.cameraX = cameraX;
        e.cameraY = cameraY;
        e.zoom = zoomScale;
        e.selecao = tipoSelecionado;
        e.direcao = direcaoAtual;
        e.construcoes = construcoes;
        
        for (Construcao c : construcoes) {
            if (c.getTipo() == Construcao.Tipo.ESTEIRA) {
                Esteira est = esteiraEm(colDe(c.getX()), rowDe(c.getY()));
                if (est != null) {
                    e.itensEsteiras.put(c, est.getItens());
                }
            }
        }
        return e;
    }

    private void aplicarEstado(GerenciadorSave.Estado e) {
        construcoes = new ArrayList<>();
        esteiras.clear();
        mapaEsteiras.clear();
        ocupacao.clear();
        construcaoInspecionada = null;
        menuConstrucaoAberto = false;

        for (Construcao c : e.construcoes) {
            adicionarConstrucao(c);
            List<Item> itens = e.itensEsteiras.get(c);
            if (itens != null) {
                Esteira est = esteiraEm(colDe(c.getX()), rowDe(c.getY()));
                if (est != null) {
                    est.getItens().addAll(itens);
                }
            }
        }

        cameraX = e.cameraX;
        cameraY = e.cameraY;
        zoomScale = e.zoom;
        tipoSelecionado = e.selecao;
        direcaoAtual = e.direcao;
    }


    private void desenharMenuPausa(int screenW, int screenH) {
        fillRectangle(0, 0, screenW, screenH, new Color(0, 0, 0, 150));

        double wx = screenW / 2.0 - PAUSA_W / 2;
        double wy = screenH / 2.0 - PAUSA_H / 2;

        fillRectangle(wx, wy, PAUSA_W, PAUSA_H, DARKGRAY);
        drawRectangle(wx, wy, PAUSA_W, PAUSA_H, BLACK);
        fillRectangle(wx, wy, PAUSA_W, 35, GRAY);
        drawText("Jogo Pausado   [P/ESC] continuar", wx + 15, wy + 10, 16, WHITE);

        desenharBotao(wx + 20, wy + 45, 160, 34, "Continuar", false);
        desenharBotao(wx + 200, wy + 45, 200, 34, "Salvar novo estado...", false);

        drawText("Estados salvos (" + arquivosSalvos.size() + ")  -  pasta: " + saves.getNomePasta() + "/",
                wx + 20, wy + 90, 13, WHITE);

        double listaY = wy + 112;
        fillRectangle(wx + 20, listaY, PAUSA_W - 40, LINHAS_VISIVEIS * ALTURA_LINHA, new Color(70, 70, 70));
        drawRectangle(wx + 20, listaY, PAUSA_W - 40, LINHAS_VISIVEIS * ALTURA_LINHA, BLACK);

        if (arquivosSalvos.isEmpty()) {
            drawText("Nenhum estado salvo ainda.", wx + 35, listaY + 15, 14, LIGHTGRAY);
        }

        for (int i = 0; i < LINHAS_VISIVEIS; i++) {
            int idx = scrollSalvos + i;
            if (idx >= arquivosSalvos.size()) {
                break;
            }

            File f = arquivosSalvos.get(idx);
            double ry = listaY + i * ALTURA_LINHA;

            if (i % 2 == 1) {
                fillRectangle(wx + 21, ry, PAUSA_W - 42, ALTURA_LINHA, new Color(80, 80, 80));
            }

            String nome = saves.nomeDe(f);
            if (nome.length() > 24) {
                nome = nome.substring(0, 22) + "...";
            }
            
            drawText(nome, wx + 30, ry + 7, 14, WHITE);
            drawText(saves.dataDe(f), wx + 30, ry + 27, 11, LIGHTGRAY);

            double by = ry + 8;
            desenharBotao(wx + 270, by, 85, 30, "Carregar", false);
            desenharBotao(wx + 360, by, 95, 30, "Salvar aqui", false);
            desenharBotao(wx + 460, by, 80, 30, "Excluir", false);
        }

        desenharBotao(wx + 20, wy + 456, 100, 30, "Anterior", false);
        desenharBotao(wx + 130, wy + 456, 100, 30, "Próxima", false);

        int de = 0;
        if (!arquivosSalvos.isEmpty()) {
            de = scrollSalvos + 1;
        }
        
        int ate = Math.min(arquivosSalvos.size(), scrollSalvos + LINHAS_VISIVEIS);
        drawText(de + "-" + ate + " de " + arquivosSalvos.size() + "   (roda do mouse rola a lista)",
                wx + 250, wy + 463, 12, LIGHTGRAY);

        if (!mensagemPausa.isEmpty()) {
            drawText(mensagemPausa, wx + 20, wy + 497, 13, YELLOW);
        }
    }

    private boolean mouseEmUI(int screenH) {
        return getMouseY() <= 35 || getMouseY() >= screenH - 34;
    }

    private void desenharSetaEsteira(double x, double y, double tamanho, Esteira.Direcao dir, Color cor) {
        double cx = x + tamanho / 2;
        double cy = y + tamanho / 2;
        double r = tamanho * 0.25;

        switch (dir) {
            case CIMA:
                fillTriangle(cx, cy - r, cx - r, cy + r, cx + r, cy + r, cor);
                break;
            case DIREITA:
                fillTriangle(cx + r, cy, cx - r, cy - r, cx - r, cy + r, cor);
                break;
            case BAIXO:
                fillTriangle(cx, cy + r, cx - r, cy - r, cx + r, cy - r, cor);
                break;
            case ESQUERDA:
                fillTriangle(cx - r, cy, cx + r, cy - r, cx + r, cy + r, cor);
                break;
        }
    }

    public static void main(String[] args) {
        new Main();
    }
}