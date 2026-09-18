<div align="center">

  <img src="composeApp/src/commonMain/composeResources/drawable/app_logo_wordmark.png" alt="NuvioTvMod" width="320" />
  <br />
  <h3>📺 NuvioTvMod (Experimental Fork)</h3>

  <p>
    <b>Versão experimental e de testes pessoais desenvolvida através de Vibe Coding com IA.</b><br />
    Baseada no incrível projeto de código aberto <a href="https://github.com/NuvioMedia/NuvioDesktop">Nuvio Desktop</a>.
  </p>

  <p>
    <a href="https://github.com/MarechalSp/NuvioTvMod/releases/latest">
      <img src="https://img.shields.io/github/v/release/MarechalSp/NuvioTvMod?style=for-the-badge&color=blue&label=Download%20Instalador" alt="Download Release" />
    </a>
    <img src="https://img.shields.io/badge/Status-Testes%20Pessoais-orange?style=for-the-badge" alt="Status" />
    <img src="https://img.shields.io/badge/Feito%20com-Vibe%20Coding%20%2B%20IA-purple?style=for-the-badge" alt="Vibe Coding" />
  </p>

</div>

---

> [!WARNING]
> ### ⚠️ Versão de Testes Pessoal (Disclaimer & Vibe Coding)
> Este projeto é uma **versão de testes pessoais** criada por pura curiosidade e experimentação prática utilizando metodologia de **Vibe Coding** (desenvolvimento assistido por Inteligência Artificial).
>
> Não se trata de uma versão oficial, comercial ou substituta do Nuvio. Pode conter instabilidades inerentes a softwares em estágio alpha experimental.

---

## 🌟 Créditos e Agradecimentos aos Criadores Originais

Este fork só existe graças ao trabalho brilhante e à dedicação de toda a equipe e dos colaboradores do projeto original **Nuvio**:

* **Repositório Oficial:** [github.com/NuvioMedia/NuvioDesktop](https://github.com/NuvioMedia/NuvioDesktop)
* **Site Oficial do Nuvio:** [nuvioapp.space](https://nuvioapp.space)
* **Organização no GitHub:** [github.com/NuvioMedia](https://github.com/NuvioMedia)
* **Termos Legais e Isenções do Projeto Original:** [nuvioapp.space/legal](https://nuvioapp.space/legal)

Todos os direitos do código original, design base e marcas pertencem aos seus respectivos autores e à comunidade do Nuvio. Incentivamos fortemente que você visite, dê estrela (⭐️) e apoie o projeto oficial!

---

## ✨ O que foi modificado nesta versão (`NuvioTvMod`)

Esta versão foi customizada para testes específicos com recursos adicionais:

1. **Aba de Canais de TV Ao Vivo:**
   * Suporte para explorar e reproduzir canais ao vivo fornecidos por addons de mídia instalados pelo usuário.
   * Guia de programação eletrônico (**EPG**) com suporte a URLs XMLTV customizáveis (sem fonte pré-definida de fábrica).
   * Controles de visualização: filtros rápidos por addons (com resposta imediata ao clique), categorias e catálogo de canais.
   * Player de TV em tela cheia com reativação automática dos controles ao mexer o cursor do mouse.

2. **Multi-idioma Completo:**
   * Interface da aba de TV totalmente traduzida para Português, Inglês e Espanhol, respeitando o idioma selecionado nas configurações do app.

3. **Compatibilidade com Tema Claro (White Mode):**
   * Ajuste fino no layout: ícones e tipografia da aba de TV escurecem automaticamente quando o aplicativo é configurado no tema branco para máxima legibilidade e conforto visual.

4. **Isolamento de Instalação (Sem Conflitos):**
   * Renomeado internamente para `NuvioTvMod`.
   * Diretórios de dados (`AppData/Roaming/NuvioTvMod`) e Cache isolados, além de novo identificador de pacote para Windows.
   * **Você pode instalar e usar o Nuvio original e o NuvioTvMod simultaneamente na mesma máquina sem que um interfira nos dados do outro.**

5. **Atualizações Automáticas Desativadas:**
   * As atualizações oficiais em segundo plano foram desabilitadas nesta versão modificada para garantir que futuros updates oficiais não sobrescrevam nem quebrem as funções experimentais de TV.

---

## 📦 Como Instalar

1. Acesse a página de versões: [GitHub Releases](https://github.com/MarechalSp/NuvioTvMod/releases/latest).
2. Baixe o instalador **`Instalador NuvioTvMod.msi`**.
3. Execute e instale no seu computador com Windows.

---

## 🛠️ Como Compilar a partir do Código Fonte

Requisitos: Java JDK 17+ instalado e configurado.

```bash
# Clonar o repositório
git clone https://github.com/MarechalSp/NuvioTvMod.git
cd NuvioTvMod

# Executar em modo desenvolvimento (Windows PowerShell)
.\gradlew.bat :composeApp:run

# Gerar o instalador MSI limpo para Windows
.\gradlew.bat :composeApp:packageReleaseMsi --rerun-tasks
```

O instalador gerado ficará em:
`composeApp/build/compose/binaries/main/msi/NuvioTvMod-Windows-x64-*.msi`

---

## ⚖️ Aviso Legal & DMCA

O NuvioTvMod funciona exclusivamente como uma interface de cliente para navegação e reprodução de conteúdos a partir de extensões e fontes adicionadas ou configuradas pelo próprio usuário. O software **não hospeda, não armazena, não distribui e não transmite nenhum conteúdo de mídia ou lista protegida por direitos autorais**. O uso é de inteira responsabilidade do usuário final.
