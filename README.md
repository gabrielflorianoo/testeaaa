# ProjectE QoL+

Addon de **qualidade de vida** para o [ProjectE](https://www.curseforge.com/minecraft/mc-mods/projecte) — **Forge 1.20.1** (ProjectE `1.20.1-PE1.0.1`).

Tudo usa a API pública do ProjectE (EMC pessoal, conhecimento, evento de aprendizado), então funciona com o seu save atual, respeita mods que cancelam o aprendizado e o EMC continua o mesmo da Mesa de Transmutação.

## Recursos

### HUD e tooltips (cliente)
- **HUD de EMC**: seu EMC pessoal, ganho/perda por segundo (média de 5 s) e quantos itens você aprendeu. Por padrão aparece só quando você carrega uma Tábua de Transmutação. Dá para mudar o canto, o deslocamento, o formato compacto (`1.25M`) e o fundo. A tecla **H** mostra/esconde.
- **Estrelas de Klein no HUD**: mostra o total de EMC guardado nas estrelas que você carrega.
- **Valor da mão no HUD**: mostra quanto vale a pilha que você está segurando.
- **Itens não aprendidos marcados**: em qualquer inventário, os itens com EMC que você ainda não aprendeu ganham um pontinho roxo no canto do slot.
- **Botões na tela de transmutação**: ao lado da GUI do ProjectE aparecem **Aprender Inventário** e **Queimar Inventário**.
- **Meta de EMC**: com `/peqolclient goal minecraft:diamond_block 64`, o HUD mostra uma barra de progresso até você conseguir pagar por aquilo. `/peqolclient goal clear` remove a meta.
- **Info ao mirar** (um "Jade-lite"): mirando numa máquina do QoL+ ou num bloco de EMC do ProjectE (coletor, relé, condensador...), aparece um painel ao lado da mira com o dono, o EMC armazenado, a variação em EMC/s e o estado. Nos condensadores mostra o alvo e o progresso; nos coletores, a luz.
- **Manual do Alquimista**: um livro dentro do jogo, traduzido, que explica tudo. É dado no primeiro login, e `/peqol handbook` dá outro.
- **Toasts de aprendizado**: uma notificação aparece quando você aprende itens novos.
- **Toasts de marco**: uma notificação aparece quando o seu EMC passa de 1K, 10K, 100K, 1M e assim por diante.
- **Amuletos ativos no HUD**: os amuletos ligados aparecem listados embaixo do EMC.
- **Tooltips extras** (segure SHIFT):
  - *Você pode transmutar: N (X pilhas)*: quantos você consegue criar com o EMC atual.
  - *Valor de venda*, quando é diferente do valor de criação.
  - *Pilha de 64: X EMC*.
  - *Conteúdo: X EMC*: soma do EMC dos itens dentro de shulker boxes e de outros blocos que guardam itens.
  - *Sem valor de EMC*, em itens que não têm EMC.

### Teclas de atalho (Opções → Controles → ProjectE QoL+)
| Tecla | Ação |
|---|---|
| **J** | Abre a interface de transmutação de qualquer lugar (precisa de uma Tábua de Transmutação no inventário) |
| **B** | Abre a primeira Bolsa Alquímica do inventário sem precisar segurá-la |
| *(sem tecla)* | **Queima rápida** (segurando agachar, queima também todas as pilhas iguais do inventário): converte a pilha da mão em EMC pessoal e aprende o item |
| **H** | Mostra/esconde o HUD de EMC |
| *(sem tecla)* | Liga/desliga o Reabastecedor, o Sifão, o Reparo, a Saciedade e o Elo Estelar (uma tecla para cada) |
| **Botão do meio** | **Pick block alquímico**: se você não tem o bloco, ele é criado com o seu EMC (exige o Reabastecedor ativo) |

### Itens
- **Reabastecedor Alquímico**: se uma ferramenta aprendida quebrar na sua mão, ele cria uma nova com EMC. Enquanto está ativo no inventário, completa as pilhas de blocos da hotbar (e da mão secundária) usando o seu EMC. Só funciona com itens aprendidos e sem NBT. Ideal para construir.
- **Amuleto de Sifão**: itens coletados que estão no filtro viram EMC pessoal na hora (e são aprendidos), mesmo com o inventário cheio. Também pega os drops filtrados dos mobs que você mata. Agache e clique com a outra mão vazia para ligar o **modo lixo**, que sifona automaticamente pedregulho, terra, cascalho, netherrack etc. (tag `#peqol:siphon_junk`, extensível por datapack). Para adicionar ou remover um item do filtro, agache e clique com o botão direito segurando o item na outra mão. Agachar e clicar com a outra mão vazia lista o filtro.
- **Amuleto de Reparo Alquímico**: repara as ferramentas e armaduras danificadas usando EMC. Um reparo completo custa cerca do valor do item.
- **Amuleto da Saciedade**: escolha uma comida aprendida (agache e clique com o botão direito segurando a comida na outra mão). Ele come automaticamente quando a fome cairia sem desperdício, pagando com EMC.
- **Elo Estelar**: mantém as Estrelas de Klein do inventário (e dos slots do Curios) carregadas com o seu EMC pessoal.
- **Amuleto da Iluminação**: coloca tochas aos seus pés em lugares escuros. Usa as tochas do inventário primeiro e depois EMC. O item colocado é configurável (por exemplo, tocha de alma).
- **Varinha de Construção Alquímica**: clique numa face e ela estende todos os blocos iguais conectados naquela face, de 1 a 81 blocos. Agache e clique no ar para mudar o tamanho. Mostra uma prévia em contorno lilás, usa os blocos do inventário e depois EMC, e respeita proteções de área.
- **Lente de Avaliação Alquímica**: clique num baú (ou qualquer inventário) para ver quanto vale o conteúdo e os 5 itens mais valiosos. Em coletores, relés e condensadores, mostra o EMC armazenado. No ar, avalia o seu inventário. Agachar e clicar **duas vezes** (em até 3 s) num baú **queima o conteúdo** em EMC.
- **Amuleto da Fundição**: os minérios e materiais brutos coletados chegam já fundidos. Cada item custa 16 EMC (configurável).
- **Âncora Alquímica**: agache e clique num bloco para ancorar o ponto; clique com o botão direito para teleportar até lá pagando EMC (base + distância, mais caro entre dimensões). Funciona entre dimensões e tem cooldown.
- **Chave do Filósofo**: agache e clique com o botão direito em qualquer máquina do ProjectE (condensador, coletor, relé, baú alquímico...) ou do QoL+ para pegá-la com o **EMC, o inventário e as configurações intactos**. O tooltip do item mostra o EMC guardado. Respeita proteções de área.
- **Prospector Alquímico**: escaneia os minérios num raio de 12 blocos e mostra quantos são, quanto valem em EMC e os mais valiosos.
- **Bancada de Bolso**: abre uma mesa de trabalho em qualquer lugar.
- **Bolsa do Vazio**: abre o seu baú do Ender em qualquer lugar, por 64 EMC (configurável).
- **Cheque de EMC**: o `/peqol withdraw` transforma EMC em um item que pode ser trocado entre jogadores. Clique com o botão direito para resgatar; agachado, você resgata a pilha inteira. O cheque não tem valor de EMC, então não dá para queimá-lo e duplicar EMC.
- **Pergaminho do Conhecimento**: grava tudo o que você aprendeu. Outro jogador clica com o botão direito e aprende tudo o que está gravado, ótimo para servidores. Agachar e clicar com o botão direito apaga o pergaminho.

### Blocos (ficam vinculados a quem os coloca)
- **Conexão Alquímica**: recebe EMC de Coletores e Relés e manda direto para o seu EMC pessoal. Enquanto você está offline, guarda o EMC num buffer.
- **Importador Alquímico**: uma "mesa de transmutação" para automação. Itens inseridos por funis ou canos viram seu EMC e são aprendidos. Itens sem EMC ficam na origem. Se você estiver offline, guarda o EMC até você entrar.
- **Exportador Alquímico**: clique nele com um item para filtrar. Ele cria os itens filtrados com o seu EMC e os empurra para os inventários vizinhos. Um sinal de redstone pausa o bloco.
  - **Modo estoque**: agache e clique com o botão direito segurando *N* itens. O exportador mantém exatamente *N* deles nos inventários vizinhos, repondo só o que for usado.
- **Aspirador Alquímico**: suga os itens soltos num raio de 6 blocos e os transforma em EMC, com filtro opcional. Ideal para farms de mobs e de árvores. Guarda o EMC se você estiver offline, e redstone pausa.
- **Descida Alquímica**: o contrário da Conexão. Puxa o seu EMC pessoal e o envia para os relés, coletores e outros blocos de EMC vizinhos, o que permite carregar Estrelas de Klein num relé usando o EMC pessoal. Redstone pausa.
- **Medidor Alquímico**: emite um sinal de comparador igual ao número de dígitos do seu EMC pessoal (1–9 EMC = 1, 1M = 7, 100T ou mais = 15). Serve para ligar e desligar farms ou máquinas conforme o seu saldo, e funciona com você offline.
- **Escavadeira Alquímica**: uma pedreira movida a EMC. Escava um quadrado de 9×9 (configurável) abaixo dela, camada por camada, até a rocha-mãe. Os drops com EMC viram o seu EMC pessoal e o resto vai para um baú vizinho. Cada bloco custa um pouco de EMC. Respeita proteções, precisa de você online, redstone pausa, agachar com a mão vazia reinicia, e aceita Melhorias de Máquina (mais blocos por ciclo).
- **Fonte Alquímica**: água ou lava infinita, paga com o seu EMC (valor do balde cheio menos o do balde vazio). Empurra o fluido para tanques e canos vizinhos e também pode ser drenada. Agache e clique com a mão vazia para alternar entre água e lava. Redstone pausa.
- **Posto de Troca Alquímico**: uma loja de jogador. Coloque o posto em cima de um baú com o estoque, clique nele com o item que quer vender e, olhando para ele, use `/peqol shop price <EMC>`. Os outros jogadores clicam com a mão vazia para comprar (agachados compram uma pilha), pagando com o EMC pessoal deles. O lucro vai para você na hora, ou fica guardado se você estiver offline. Agache e clique com a mão vazia para trocar para o **modo compra**: os jogadores clicam segurando o produto, os itens vão para o baú e você paga em EMC (precisa estar online).
- **Interface Alquímica**: transforma o seu conhecimento num **inventário virtual**. Cada item aprendido aparece como um slot, com a quantidade que você consegue pagar. Canos, funis e mods de armazenamento (storage bus do **AE2**, external storage do **Refined Storage**) podem puxar itens, que são criados com o seu EMC, e inserir itens, que são queimados. Com o AE2 isso vira autocrafting movido a EMC. Funciona enquanto o dono está online.
- **Melhoria de Máquina Alquímica**: clique com ela num Exportador, Aspirador ou Descida para subir o nível da máquina (até 3). O Exportador fica mais rápido e move mais itens; o Aspirador fica mais rápido e ganha +2 de raio por nível; a Descida move mais EMC. O nível é mantido ao usar a chave.
- A **Conexão Alquímica** emite sinal de comparador proporcional ao buffer.

Clicar com a mão vazia mostra dono, estado e estatísticas no chat.

### Comandos `/peqol`
| Comando | Descrição |
|---|---|
| `balance [jogador]` | Mostra o EMC (ver o de outro jogador exige OP) |
| `knowledge [jogador]` | Mostra quantos itens o jogador aprendeu e o progresso em % do total de itens com EMC |
| `pay <jogador> <quantia>` | Envia EMC para outro jogador. Aceita `1.5k`, `20M`, `3B`... |
| `withdraw <quantia> [cheques]` | Saca o EMC em Cheques de EMC |
| `worth [hand\|inventory]` | Mostra quanto vale a mão ou o inventário |
| `sell <item> [qtd]` | Queima uma quantidade exata de um item do inventário (sem quantidade, queima todos) |
| `xp toemc [pontos]` / `xp fromemc <pontos>` | Converte experiência em EMC e vice-versa (32 EMC por ponto, configurável) |
| `gift <jogador> <item> [qtd]` | Cria um item aprendido com o seu EMC direto no inventário de outro jogador |
| `share <jogador>` / `accept` | Oferece todo o seu conhecimento a outro jogador; ele aceita clicando em **[Aceitar]** no chat ou com `/peqol accept` |
| `tablet` | Abre a transmutação (igual à tecla J) |
| `bag [cor]` | Abre uma bolsa alquímica, opcionalmente escolhendo a cor (ex.: `/peqol bag red`) |
| `home set\|del <nome>` / `home [list]` / `home <nome>` | Casas com nome (até 5, configurável). A lista tem botões clicáveis e o teleporte custa EMC como a Âncora |
| `fav add\|remove <item>` / `fav` | Itens favoritos com botões de compra rápida (também existe tecla) |
| `shop price <EMC>` | Define o preço do Posto de Troca que você está olhando |
| `back` | Teleporta para onde você morreu por último, pagando EMC (mesma fórmula da Âncora). Procura um lugar seguro, sem lava |
| `stats` | Mostra as suas estatísticas do QoL+ |
| `burn hand\|inventory` | Queima a mão ou o inventário principal. A hotbar, a armadura, a mão secundária, os itens com NBT e as Estrelas de Klein são preservados |
| `buy <item> [qtd]` | Cria itens aprendidos com o seu EMC, com autocompletar (ex.: `/peqol buy minecraft:diamond 64`) |
| `value <item>` | Mostra o valor de qualquer item, se você já o aprendeu e quantos consegue criar |
| `known [page N \| search <texto>]` | Navega pelos itens aprendidos, com botões **[x1] [x64]** clicáveis no chat para comprar na hora |
| `autostock add <item> <qtd>` / `remove` / `list` / `clear` | Mantém sempre *qtd* daquele item no inventário (flechas, tochas, comida...), criando o que faltar com EMC. Precisa do Reabastecedor ativo e sobrevive à morte |
| `learnall` | Aprende tudo o que ainda não conhece no inventário, queimando **só 1 de cada** item e mantendo o resto (também existe como tecla) |
| `unlearned` | Lista os itens do inventário com EMC que você ainda não aprendeu |
| `stars fill\|drain` | Enche as Estrelas de Klein com o seu EMC, ou esvazia todas no seu EMC |
| `help` | Lista os comandos |
| `missing` | Lista os itens do inventário que não têm EMC |
| `top` | Ranking de EMC dos jogadores online |

## Receitas
| Item | Receita |
|---|---|
| Reabastecedor Alquímico | Pó de Covalência Médio ×4, Matéria Escura ×4, Baú |
| Amuleto de Sifão | Pó de Covalência Baixo ×4, Funil ×4, Faixa de Ferro |
| Pergaminho do Conhecimento | Papel ×2, Bolsa de Tinta, Pó de Covalência Médio (sem forma) |
| Conexão Alquímica | Matéria Escura ×2, Relé MK1 ×2, Mesa de Transmutação |
| Importador Alquímico | Matéria Escura ×2, Funil ×2, Mesa de Transmutação |
| Exportador Alquímico | Matéria Escura ×2, Liberador ×2, Mesa de Transmutação |
| Amuleto de Reparo Alquímico | Pó de Covalência Médio ×2, Matéria Escura ×2, Talismã de Reparo |
| Amuleto da Saciedade | Pó de Covalência Baixo ×4, Cenoura Dourada ×4, Faixa de Ferro |
| Varinha de Construção | Matéria Escura, Vara de Blaze, Pó de Covalência Médio (na diagonal) |
| Amuleto da Iluminação | Pó de Covalência Baixo ×4, Tocha ×4, Faixa de Ferro |
| Lente de Avaliação | Ouro ×3, Painel de Vidro, Pó de Covalência Baixo |
| Elo Estelar | Pó de Covalência Alto ×2, Estrela de Klein Ein ×2, Faixa de Ferro |

| Aspirador Alquímico | Faixa do Buraco Negro, Funil ×2, Importador Alquímico, Matéria Escura |
| Âncora Alquímica | Pérola do Ender, Pó de Covalência Médio ×2, Bússola, Matéria Escura |
| Descida Alquímica | Matéria Escura ×2, Tocha de Redstone ×2, Conexão Alquímica |
| Chave do Filósofo | Ferro ×2, Matéria Escura, Pó de Covalência Médio |
| Medidor Alquímico | Comparador, Redstone ×2, Matéria Escura, Pó de Covalência Baixo |
| Prospector Alquímico | Linha, Pepita de Ouro, Pó de Covalência Baixo ×2, Diamante |
| Bancada de Bolso | Mesa de Trabalho, Pó de Covalência Baixo, Couro (sem forma) |
| Bolsa do Vazio | Linha, Couro ×3, Baú do Ender |
| Melhoria de Máquina (×2) | Pó de Covalência Alto ×4, Redstone ×4, Matéria Escura |
| Escavadeira Alquímica | Matéria Escura ×4, Picareta de Matéria Escura, Matéria Vermelha ×2, Aspirador Alquímico, Funil |
| Fonte Alquímica | Matéria Escura ×2, Amuleto Evertide, Amuleto Vulcanita, Balde |
| Posto de Troca | Lã ×3, Ouro ×2, Esmeralda, Tábuas ×3 |
| Interface Alquímica | Matéria Vermelha ×4, Baú ×2, Importador, Exportador, Mesa de Transmutação |
| Amuleto da Fundição | Pó de Covalência Baixo ×4, Fornalha ×4, Faixa de Ferro |

## Extras
- **Estatísticas**: em Estatísticas → Geral aparecem os itens queimados, os itens criados com EMC, os blocos construídos com a varinha, os itens fundidos, os reparados, as luzes colocadas, os teleportes e as refeições alquímicas.
- **Conquistas**: tem uma aba própria "ProjectE QoL+" com 11 conquistas.
- **Placar de EMC**: o objetivo `peqol_emc` é atualizado com o EMC de cada jogador. Para exibir: `/scoreboard objectives setdisplay sidebar peqol_emc`.
- **Conhecimento inicial**: `server.starterKnowledge` é uma lista de itens que todo jogador já conhece ao entrar pela primeira vez.
- **Aprender ao fabricar** (`server.learnOnCraft`, desligado por padrão): fabricar um item com EMC também o aprende.
- **Desativar receitas**: em `blocks.disabledRecipes` você lista os itens do addon que não podem ser fabricados (por exemplo `["alchemical_anchor"]`).
- **Lista negra para servidores**: `blacklist.items` impede que itens sejam criados com EMC pelo `buy`, pelo pick block, pelo Reabastecedor, pelo Exportador e pela varinha. Aceita IDs e tags, por exemplo `["minecraft:elytra", "#forge:ores"]`.

## JEI (opcional)
Com o JEI instalado, todos os itens do addon ganham uma página de informação (aperte **U** ou **R** sobre o item).

## Jade (opcional)
Com o Jade instalado, as informações das máquinas do QoL+ (dono, buffer, nível, estado) e dos blocos de EMC do ProjectE (EMC armazenado, variação, alvo e progresso dos condensadores) aparecem **dentro do tooltip do Jade**, e o painel próprio do addon se esconde.

## Curios (opcional)
Se o [Curios](https://www.curseforge.com/minecraft/mc-mods/curios) estiver instalado, os itens podem ficar nos slots dele e continuam funcionando:
- **Cinto**: Reabastecedor, Iluminação e Tábua de Transmutação.
- **Colar**: Sifão e Saciedade.
- **Anel**: Reparo, Elo Estelar e Fundição.

## Ajustes no jogo (`/peqolclient`)
Comandos só do cliente, que salvam na config na hora:
- `/peqolclient hud corner top_left|top_right|bottom_left|bottom_right`
- `/peqolclient hud mode always|tablet_in_inventory|never`
- `/peqolclient hud offset <x> <y>`
- `/peqolclient hud compact|rate|charms|held|stars|background` (liga/desliga cada parte do HUD)
- `/peqolclient tooltips affordable|sellvalue|contents|noemc`
- `/peqolclient goal <item> [qtd]` / `goal clear`
- `/peqolclient history`: mini-gráfico do seu EMC nos últimos 10 minutos
- `/peqolclient machineinfo|learntoasts|milestones|markunlearned`

## Configuração
- `config/peqol-common.toml`: permite desativar `pay`, `withdraw`, `burn` e a queima rápida, e definir se é preciso ter a Tábua. Também controla o intervalo e o limite do Reabastecedor, o buffer da Conexão, a velocidade do Exportador, o aprender ao coletar (desligado por padrão) e outras opções.
- `config/peqol-client.toml`: HUD e tooltips.

## Releases
Ao enviar uma tag `v*` (por exemplo `git tag v1.5.0 && git push origin v1.5.0`), o GitHub Actions compila e publica o jar como uma release. O [CHANGELOG](CHANGELOG.md) lista as mudanças de cada versão.

## Compilando
```bash
./gradlew build
```
O jar fica em `build/libs/`. O GitHub Actions compila, roda os game tests (um servidor headless com o ProjectE carregado) e publica o jar como artefato.

---

# ProjectE QoL+ (English)

A quality-of-life addon for ProjectE on Forge 1.20.1. It includes:
- An EMC HUD with the EMC/s rate.
- Keybinds to open the transmutation GUI and alchemical bags from your inventory, plus a quick-burn keybind.
- Extra tooltips: how many you can afford, sell value, EMC of container contents.
- An Alchemical Restocker that refills the hotbar from personal EMC.
- A Siphon Charm that turns filtered pickups into EMC.
- Tradeable EMC Cheques and Knowledge Scrolls for sharing knowledge.
- Uplink, Importer and Exporter blocks that bridge automation and personal EMC.
- Alchemical Excavator (EMC-powered quarry).
- Alchemical Spring (water/lava paid with EMC for pipes and tanks).
- Alchemical Trade Post (player shops paid in EMC), mod list logo.
- Alchemical Interface (learned items as a virtual inventory for pipes/AE2/RS), per-recipe disabling via config.
- Jade integration (machine info inside Jade's tooltip).
- Named homes (`/peqol home`) and favorites with quick-buy buttons (`/peqol fav` + keybind).
- Machine upgrades (tiers 1-3 for Exporter, Vacuum, Downlink) and a peqol_emc scoreboard objective.
- Unlearned-item slot markers, Siphon junk mode, CHANGELOG and tag-based releases.
- Buttons on the transmutation screen (learn/burn inventory), EMC goal progress bar on the HUD.
- JEI info pages for every item, `/peqol tablet` and `/peqol bag [color]`.
- `/peqol share` + `/peqol accept` (knowledge sharing with consent), `/peqolclient` to tweak HUD and tooltips in game.
- Alchemist's Handbook (in-game guide book), richer look-at info (condenser target/progress, collector light, EMC/s).
- Alchemical Prospector, `/peqol gift`, starter knowledge and learn-on-craft server options.
- Pocket Workbench, Void Pouch, `/peqol back`, Siphon catching mob drops.
- Look-at machine info panel, `/peqol learnall` (+ keybind), lens container burning with confirmation.
- Alchemical Gauge (comparator by EMC magnitude), `/peqol sell|xp|stats`, sneak quick-burn, learn toasts.
- Philosopher's Wrench (pick up ProjectE machines with EMC/contents), Alchemical Downlink, `/peqol autostock`.
- Alchemical Vacuum, Alchemical Anchor (EMC teleport), Charm of Smelting, broken-tool replacement, custom statistics, advancements and a server blacklist.
- Construction Wand with preview, Charm of Illumination, Appraisal Lens.
- `/peqol known` with clickable buy buttons, knowledge progress %, held-item value on the HUD and EMC milestone toasts.
- Alchemical pick block (middle click creates the block from EMC).
- Mending, Satiation and Stellar Link charms.
- Stock-keeping mode for the Exporter and a comparator output for the Uplink.
- Optional Curios support.
- `/peqol` commands: help, balance, pay, withdraw, buy, value, worth, burn, missing, unlearned, stars, top, knowledge.

Fully translated to English and Brazilian Portuguese.
