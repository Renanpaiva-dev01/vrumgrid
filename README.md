# VrumGrid

Jogo de lógica para Android (Kotlin + Jetpack Compose) inspirado nas mecânicas do Meowdoku,
com tema, nome, ilustrações, sons e código próprios. O tabuleiro é o mapa de uma cidade vista de
cima: o objetivo é **estacionar um carro em cada linha, cada coluna e cada bairro colorido**, sem
que dois carros se encostem (nem na diagonal).

Ao estacionar, o **carrinho do jogador percorre sozinho o tabuleiro** e deixa um X em cada vaga
que ficou impossível. O carrinho pode ser personalizado na **Garagem** (modelo, pintura, cor dos
detalhes, adesivo e número).

> O nome interno do projeto continua `MiauGrid` (pacote `com.miaugrid`, classes `MiauGridApp`,
> `MiauGridTheme`), para não mudar o applicationId nem os dados salvos. O nome exibido é VrumGrid.

> **Situação da build (leia primeiro).** O APK é compilado pelo GitHub Actions a cada push. Lá
> os 88 testes do núcleo e os 14 testes de interface passam num emulador Android (API 34).
> Ainda **não houve revisão visual feita por uma pessoa num aparelho real**. Detalhes em
> [Resultado real da verificação](#resultado-real-da-verificação).

---

## Como compilar e executar

Requisitos: JDK 17 ou mais novo, Android SDK com a plataforma 36 (Android Studio recente) e
acesso ao Google Maven.

```bash
cp local.properties.example local.properties   # ajuste sdk.dir
./gradlew :app:assembleDebug                    # APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:installDebug                     # instala no aparelho/emulador conectado
./gradlew -p core test                          # testes do núcleo (não precisa de Android SDK)
./gradlew :app:connectedDebugAndroidTest        # testes de interface (precisa de emulador)
```

A build de depuração usa o sufixo `.debug` no applicationId e **somente anúncios de teste**.

### Compilar e testar online (sem Android Studio)

**GitHub Actions** (`.github/workflows/android.yml`): ao enviar o projeto para um repositório no
GitHub, cada push roda os testes do núcleo e compila o APK de depuração nos servidores do GitHub.
O APK fica na execução, em *Actions → (execução) → Artifacts → vrumgrid-debug-apk* (um .zip com o
.apk). Os testes de interface rodam num emulador a cada push na `main`, ou manualmente em
*Actions → Android → Run workflow*. Em repositório público os minutos são gratuitos; em privado
entram na cota gratuita mensal do GitHub.

**Testar no navegador:** envie o `app-debug.apk` para um emulador on-line como o Appetize.io
(tem plano gratuito com minutos limitados) e jogue pelo navegador, ou instale o APK direto num
celular Android (é preciso permitir "instalar apps desconhecidos"). Os anúncios de teste do Google
podem não carregar em emuladores on-line; o jogo funciona sem eles.

## Arquitetura

```
core/   Kotlin/JVM puro (sem Android). Regras, solucionador, partida, dicas, anúncios
        (interfaces), progressão, estatísticas, desafio diário e persistência versionada.
app/    Android: telas Compose, ViewModels, AdMob + UMP, DataStore, sons, navegação.
tools/ui-typecheck/   checagem de tipos do app sem Android SDK (ver README da pasta).
```

O `core` é uma build Gradle independente incluída por `includeBuild("core")`. Assim, o motor de
regras é compilado e testado sem iniciar nenhuma tela Android.

| Responsabilidade | Onde |
|---|---|
| Modelo do tabuleiro e das fases | `core/.../model` (`PuzzleLayout`, `Puzzle`, `Solution`) |
| Motor de regras e validação central | `core/.../rules/RulesEngine.kt` |
| Solucionador (backtracking, conta até 2) | `core/.../solver/Solver.kt` |
| Deduções lógicas (dificuldade e dicas) | `core/.../solver/DeductionEngine.kt` |
| Gerador de fases | `core/.../generator/PuzzleGenerator.kt` |
| Conteúdo e gerenciador de fases | `core/.../levels`, `core/src/main/resources/levels/campaign.json` |
| Estado da partida, vidas, desfazer, reinício | `core/.../game/GameSession.kt` (função pura estado + ação) |
| Dicas | `core/.../hints/HintEngine.kt` |
| Recuperação por anúncio | `core/.../recovery/RecoveryCoordinator.kt` |
| Publicidade (interface, frequência) | `core/.../ads` + `app/.../ads/AdMobProvider.kt` |
| Progressão e desbloqueio | `core/.../progress` |
| Estatísticas | `core/.../stats` |
| Desafio diário | `core/.../daily` |
| Persistência versionada | `core/.../persistence` + `app/.../data/DataStoreKeyValueStore.kt` |
| Configurações | `core/.../settings` + `app/.../ui/settings` |
| Telemetria (somente local) | `core/.../telemetry` + `app/.../telemetry` |
| Marcação automática pelo carro | `core/.../game/GameSession.kt` (`autoMarksFor`, `PlaceCat(autoMark)`) |
| Carrinho personalizável | `core/.../settings/UserSettings.kt` (`CarStyle`), `app/.../ui/components/CarDrawing.kt`, `app/.../ui/garage` |
| Áudio, animações, efeitos | `app/.../audio/SoundPlayer.kt`, `app/.../ui/game/GameBoard.kt`, `app/.../ui/components/Road.kt` |
| Interface e navegação | `app/.../ui` (MVVM: um ViewModel por tela, `StateFlow`) |

## Tema de carros

- **Tabuleiro**: cada bairro (região) é um lote colorido com cantos arredondados, unido aos
  vizinhos do mesmo bairro. Entre bairros diferentes há asfalto com faixa tracejada.
- **Carros estacionados**: os carros do jogador usam o modelo e o adesivo escolhidos na garagem,
  pintados com o tom escuro do bairro (a cor continua indicando o bairro). O carrinho que
  percorre o tabuleiro usa a pintura completa da garagem. Carros fixos da
  fase são vans cinzas com cadeado. Estacionar faz o carro "cair" na vaga com mola e giro.
- **Erro**: aparece um cone no lugar, o cone treme, os carros em conflito ganham um anel tracejado
  vermelho e a mensagem explica a regra quebrada.
- **Vidas** são pneus: o pneu perdido murcha e fica tracejado (forma, não só cor).
- **Progresso**: uma estradinha com o carro andando até a bandeira quadriculada.
- **Abertura**: o carrinho do jogador entra derrapando e freia no centro.
- **Início**: estrada animada, botões com escala ao pressionar e entrada em cascata.
- **Vitória**: confete com bandeirinhas e buzina. Transições laterais entre as telas.
- **Sons** sintetizados no aparelho: "vrum" de motor ao estacionar com marcação automática,
  buzina de dois tons na vitória.

### Marcação automática pelo carrinho

Ligada por padrão (Configurações → Carrinho). Ao estacionar um carro aceito, o núcleo calcula as
vagas que ficaram impossíveis: mesma linha, mesma coluna, mesmo bairro e as 8 vizinhas. Ficam de
fora as vagas que já têm carro ou X. A ordem é uma rota gulosa pelo vizinho mais próximo, a partir
do carro (`GameSession.autoMarksFor`).

- Os X entram **na mesma jogada** da colocação (`Move.PlaceCat.autoMarked`). Por isso um único
  "desfazer" tira o carro e todos os X dele, e a partida salva fica sempre consistente.
- A interface só atrasa a exibição: cada X aparece quando o carrinho passa pela vaga. Velocidades:
  passeio (150 ms por vaga), normal (95 ms) e turbo (50 ms).
- Se a jogada vencer a fase, a tela de vitória espera o fim do percurso (no máximo 2,5 s).
- Colocações recusadas não marcam nada. No modo `STRICT_SOLUTION` os X automáticos nunca cobrem
  uma vaga da solução. Isso é testado em todas as 60 fases (`AutoMarkTest`).

### Garagem

Seis modelos (sedã, esportivo, buggy, picape, van, fórmula), 12 pinturas prontas mais um seletor
de tom livre, 6 cores de detalhe, 5 adesivos (liso, faixas, número, xadrez, chamas) e número de
corrida de 00 a 99. A prévia gira numa plataforma e roda numa estrada. Tudo é desenhado em
vetor no Canvas, sem imagens de terceiros. As mudanças são salvas sozinhas (com agrupamento de
300 ms, para o seletor de cor não gravar a cada quadro) e valem em todas as telas. O carrinho
fica em `UserSettings.car`. Configurações antigas sem esse campo continuam abrindo, com o carro
padrão.

## Regras e políticas

Validação central em `RulesEngine.checkPlacement`, que devolve violações estruturadas:
`OUT_OF_BOUNDS`, `FIXED_CELL`, `CELL_OCCUPIED`, `ROW_OCCUPIED`, `COLUMN_OCCUPIED`,
`REGION_OCCUPIED`, `ADJACENT_HORIZONTAL`, `ADJACENT_VERTICAL`, `ADJACENT_DIAGONAL` e
`NOT_IN_SOLUTION`, cada uma com a célula em conflito. A vizinhança usa
`max(|r1-r2|, |c1-c2|) <= 1`.

A vitória (`RulesEngine.evaluate`) exige a quantidade certa de carros, um por linha, coluna e
bairro (região), nenhum par vizinho e igualdade com a solução oficial. Um tabuleiro só "cheio" não vence.

**Política de validação por modo** (`GameModeConfig`, `ValidationPolicy`):

- `STRICT_SOLUTION` (modo clássico e desafio diário): um carro só é aceito se estiver na solução
  oficial. Qualquer outro, mesmo respeitando as regras locais, é recusado e custa 1 vida.
- `RULES_ONLY` (disponível na arquitetura, sem tela própria): só quebras de regra custam vida.
  Carros válidos fora da solução ficam no tabuleiro sem penalidade, e a dica aponta o erro.

**Outras políticas** (centralizadas em `GameSession` e testadas):

- 3 vidas iniciais e contadores separados de vidas e erros. Com 0 vidas a partida entra em
  derrota uma única vez e bloqueia jogadas até a recuperação ou o reinício.
- Marcações X são anotações: nunca custam vida e nunca são tratadas pelo motor como
  "impossível". O bloqueio confirmado pelo motor aparece como pontos, separado dos X.
- **Desfazer não devolve vida.** Colocações recusadas não entram no tabuleiro nem no histórico,
  então só jogadas válidas (carro, remoção, X, arrasto de X) podem ser desfeitas.
- Reiniciar restaura carros fixos, células, marcações, vidas, histórico, estado e cronômetro.
  As dicas grátis não são recarregadas, para não virarem dicas infinitas.
- Recuperação (`RecoveryPolicy`): habilitada, 1 por partida, devolve 1 vida (parâmetros
  configuráveis). Só é concedida com o callback de recompensa do SDK, vinculada ao id do evento
  de derrota e aplicada no máximo uma vez.
- Dicas (`HintPolicy`): 3 grátis por fase no clássico e 1 no diário. Depois disso, opcionalmente,
  uma dica por anúncio recompensado. Revelar posição pode ser ligado ou desligado por modo.

## Controles

- **Toque**: marca ou desmarca X. **Toque duplo**: estaciona um carro, ou retira um carro do jogador.
  O detector espera o intervalo de toque duplo do sistema antes de confirmar o toque simples,
  então um toque duplo nunca vira "X + carro".
- **Arrastar**: marca (ou desmarca) várias células numa única jogada desfazível. O arrasto
  consome os eventos e cancela o detector de toque, evitando gestos concorrentes.
- Modo alternativo nas configurações: escolher a ferramenta (X ou carro) numa barra. O toque
  duplo continua estacionando, sem conflito.
- Botões: desfazer, dica, reiniciar (com confirmação configurável) e pausa (continuar,
  reiniciar, configurações, sair).

## Fases

São 60 fases em `core/src/main/resources/levels/campaign.json` (versão de conteúdo 1),
de 5x5 a 10x10: 11 fáceis, 24 médias, 23 difíceis e 2 de especialista. Cada fase guarda id,
número, tamanho, regiões, carros iniciais, solução oficial e dificuldade.

Elas foram geradas por `./gradlew -p core generateLevels` (sementes fixas, reprodutível). O
teste `LevelPackTest` confirma, para todas as fases, que a solução oficial obedece às regras,
que o solucionador encontra **exatamente uma** solução, que ela é a oficial e que a dificuldade
declarada bate com o classificador. `HintEngineTest` resolve todas as fases usando só as dicas,
sem nenhum erro.

A dificuldade vem das técnicas lógicas necessárias: eliminação simples e candidato único (fácil),
confinamento de um grupo (médio), confinamento de vários grupos ou contradição (difícil), e
"exige tentativa" (especialista).

## Publicidade

A camada `core/ads` define `AdProvider` e `AdService`. O jogo não conhece o SDK. O `AdService`
impede anúncios sobrepostos, aplica a frequência dos intersticiais (padrão: 1 a cada 3 fases
concluídas e no mínimo 3 minutos entre eles), converte falhas do SDK em resultados tratáveis e
nunca bloqueia o jogo. Intersticiais só aparecem ao sair de uma fase vencida, nunca durante uma
jogada. O estado da partida é salvo antes de qualquer anúncio.

`AdMobProvider` (Google Mobile Ads 24.4.0) implementa a interface. O consentimento é obtido com
o User Messaging Platform antes de pedir anúncios.

**IDs de teste (build debug):** os IDs oficiais de teste do Google, definidos em
`app/build.gradle.kts`:

- App: `ca-app-pub-3940256099942544~3347511713`
- Recompensado: `ca-app-pub-3940256099942544/5224354917`
- Intersticial: `ca-app-pub-3940256099942544/1033173712`

**Para produção:**

1. Crie o app e os blocos de anúncio no AdMob.
2. Ponha os IDs em `local.properties` (não versionado) ou em propriedades do Gradle no CI:
   `miaugrid.admob.appId`, `miaugrid.admob.rewardedId`, `miaugrid.admob.interstitialId`.
3. Configure o formulário de consentimento (GDPR/UMP) em *Privacy & messaging* no AdMob.
4. Gere a release (`./gradlew :app:bundleRelease`) com seu próprio `signingConfig`.

Sem os três IDs, a release sai **com a publicidade desligada** (`ADS_ENABLED=false`). A
publicidade **não está pronta para produção** enquanto depender dos IDs de teste.

## Persistência

DataStore (Preferences) guarda documentos JSON versionados, um por tipo de dado: progresso,
estatísticas, configurações (incluindo o carrinho), desafio diário e uma partida salva por modo (carros, X, vidas,
erros, tempo, histórico de desfazer, recuperações). Cada documento tem um número de versão e
migrações (`Document.migrations`). Cada gravação mantém uma cópia de segurança do valor
anterior (`<chave>.bak`) na mesma operação atômica. Um documento ilegível é guardado em
`<chave>.corrupt` e restaurado da cópia, sem afetar os demais. A partida é salva a cada jogada,
a cada 15 s de cronômetro, ao pausar e ao minimizar o app. Ao reabrir, ela é restaurada, desde
que seja compatível com a fase e a versão do conteúdo.

## Desafio diário e estatísticas

O desafio diário é gerado no aparelho a partir da data, de forma determinística e offline (6x6 a
8x8, conforme o dia da semana). Cada data conta uma vez. Se o relógio voltar para antes da maior
data já vista, as conclusões não contam para a sequência. **Limitação:** sem servidor não dá
para impedir que alguém adiante o relógio para jogar desafios futuros. Um servidor de horário e
validação seria necessário para recompensas ou ranking.

As estatísticas são **somente locais**: partidas iniciadas e concluídas (vitórias + derrotas),
vitórias, derrotas, erros, tempo de jogo, melhor tempo (geral e por tamanho), dicas,
recuperações e sequência de dias. Não há ranking global.

## Privacidade

- O jogo não coleta dados pessoais nem usa conta ou login. O progresso fica só no aparelho
  (incluído no backup do Android).
- A telemetria (`Analytics`) é local: em builds de depuração vai para o Logcat e nada é enviado.
- O Google Mobile Ads SDK pode coletar o identificador de publicidade, o endereço IP, dados de
  interação com anúncios e informações do aparelho, e declara a permissão `AD_ID`. Isso precisa
  constar na seção *Data safety* da Play Store e numa política de privacidade publicada. Detalhes:
  https://developers.google.com/admob/android/privacy/play-data-disclosure
- Não há chaves, senhas ou IDs de produção no repositório.

## Funcionalidades concluídas (no código)

- Motor de regras com validação estruturada e verificação completa de vitória.
- Solucionador (contagem até 2, detecção de puzzles impossíveis ou com várias soluções, carros
  iniciais e verificação da solução oficial).
- Motor de deduções lógicas, classificador de dificuldade e gerador de fases com solução única.
- 60 fases validadas.
- Partida completa: vidas, erros, derrota única, recuperação, desfazer, reinício, cronômetro,
  dicas explicadas, revelação, marcação por arrasto e toque duplo.
- Marcação automática pelo carrinho, com animação do percurso e desfazer atômico.
- Telas: abertura, inicial, seleção de fases, partida, pausa, vitória, derrota, recuperação por
  anúncio, oferta de dica, configurações, estatísticas, desafio diário e garagem.
- Progressão com desbloqueio sequencial, recordes e "Continuar".
- Persistência versionada com cópia de segurança e restauração da partida.
- Anúncios recompensados e intersticiais modulares, com consentimento UMP.
- Acessibilidade de cores: padrões e letras por bairro, X e pontos com formas diferentes, carro
  fixo com cadeado, erros em texto e com forma (cone e anel tracejado), descrições para leitores de tela nas vidas,
  fases e tabuleiro.
- Sons originais sintetizados (motor, buzina), vibração e animações (carro percorrendo o tabuleiro,
  estacionamento com mola, cone tremendo, dica pulsando, confete, transições entre telas).
- Tema claro e escuro, layout responsivo (tabuleiro quadrado limitado a 560 dp).

## Pendências explícitas

- **Jogar num aparelho real** e revisar a interface (o APK roda no emulador do CI, mas ninguém
  avaliou o visual ainda).
- Navegação por leitor de tela célula a célula no tabuleiro: hoje o tabuleiro tem uma
  descrição geral, mas as células não são focáveis individualmente.
- IDs de produção do AdMob, política de privacidade publicada e assinatura de release.
- Modo infinito, desafios cronometrados e eventos especiais: só aparecem como "em
  desenvolvimento" na tela inicial, sem botões.
- Ranking global e validação em servidor: não implementados. As estatísticas são locais.
- O modo `RULES_ONLY` existe e é testado no núcleo, mas não tem tela para escolhê-lo.
- Revisão visual em aparelhos reais (tamanhos de fonte, telas pequenas, tablets). Os testes de
  interface verificam comportamento, não aparência.
- No emulador do perfil `pixel_6` (API 34) o emulador caiu durante o teste de derrota. Com o
  perfil padrão, tudo passa. A causa não foi investigada (pode ser do emulador).
- Ícone do app: vetor novo (quarteirões com um carrinho), não visto num launcher real.

## Resultado real da verificação

Ambiente: Linux, JDK 21, Gradle 8.14.3. A rede bloqueou `dl.google.com`, de onde vêm o Android
SDK e os artefatos do Google Maven, incluindo o Android Gradle Plugin.

| Verificação | Resultado |
|---|---|
| `./gradlew -p core test` | **88 testes, 0 falhas** (AdsAndRecovery 10, AutoMark 9, GameSession 22, Generator 2, HintEngine 4, LevelPack 4, Persistence 7, ProgressStatsDaily 8, RulesEngine 15, Solver 7) |
| `./gradlew :app:assembleDebug` | Falha no ambiente de desenvolvimento (Google Maven bloqueado). **Passa no GitHub Actions** (JDK 17, ubuntu-latest). |
| Checagem de tipos do app e dos testes de interface (`tools/ui-typecheck`) | Compila sem erros contra Compose Multiplatform Desktop 1.8.2 (inclui o tema de carros, a garagem e os testes novos). Encontrou e corrigiu 2 bugs reais (inferência de tipo em `withContext` e variável fora de escopo). APIs Android, AdMob, UMP, DataStore e androidx.lifecycle foram checadas só contra stubs. |
| `./gradlew :app:connectedDebugAndroidTest` (GitHub Actions, emulador API 34, perfil padrão) | **14 testes, 0 falhas** (GameFlow 7, GameScreen 5, GarageFlow 2). Os anúncios ficam desligados durante esses testes (`VrumGridTestRunner`). |

Os 22 cenários obrigatórios de teste estão cobertos no núcleo: linha, coluna e região (1 a 3),
adjacência horizontal, vertical e diagonal (4 a 6), carros fixos (7), coordenadas inválidas (8),
solução correta e incompleta (9 e 10), puzzle sem solução e com várias soluções (11 e 12),
consumo de vidas, derrota e vitória (13 a 15), desfazer e reiniciar (16 e 17), salvamento e
restauração (18), recuperação sem e com recompensa (19 e 20), recompensa duplicada (21) e falhas
de carregamento de anúncio (22).

## Versões das dependências

Escolhidas como um conjunto estável e compatível de meados de 2025, conferido nas páginas
oficiais de release: AGP 8.11.1 (Gradle 8.13+), Kotlin 2.2.0, Compose BOM 2025.06.00,
activity-compose 1.10.1, lifecycle 2.9.1, navigation-compose 2.9.0, datastore 1.1.7,
core-ktx 1.16.0, play-services-ads 24.4.0, user-messaging-platform 3.2.0, kotlinx-coroutines
1.10.2, kotlinx-serialization 1.9.0, androidx.test.ext:junit 1.2.1 e espresso 3.6.1.
Existem versões mais novas (por exemplo, AGP 9.x, que muda a configuração do Kotlin). Atualize
pelo Android Studio depois da primeira build.
