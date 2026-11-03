# ProjectE QoL+

Addon de **qualidade de vida** para o [ProjectE](https://www.curseforge.com/minecraft/mc-mods/projecte) — **Forge 1.20.1** (ProjectE `1.20.1-PE1.0.1`).

Tudo usa a API pública do ProjectE (EMC pessoal, conhecimento, evento de aprendizado), então funciona com o seu save atual, respeita mods que cancelam o aprendizado e o EMC continua o mesmo da Mesa de Transmutação.

## Recursos

### HUD e tooltips (cliente)
- **HUD de EMC**: seu EMC pessoal, ganho/perda por segundo (média de 5 s) e quantos itens você aprendeu. Por padrão aparece só quando você carrega uma Tábua de Transmutação. Dá para mudar o canto, o deslocamento, o formato compacto (`1.25M`) e o fundo. A tecla **H** mostra/esconde.
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
| *(sem tecla)* | **Queima rápida**: converte a pilha da mão em EMC pessoal e aprende o item |
| **H** | Mostra/esconde o HUD de EMC |
| *(sem tecla)* | Liga/desliga o Reabastecedor Alquímico |
| *(sem tecla)* | Liga/desliga o Amuleto de Sifão |

### Itens
- **Reabastecedor Alquímico**: enquanto está ativo no inventário, completa as pilhas de blocos da hotbar (e da mão secundária) usando o seu EMC. Só funciona com itens aprendidos e sem NBT. Ideal para construir.
- **Amuleto de Sifão**: itens coletados que estão no filtro viram EMC pessoal na hora (e são aprendidos), mesmo com o inventário cheio. Para adicionar ou remover um item do filtro, agache e clique com o botão direito segurando o item na outra mão. Agachar e clicar com a outra mão vazia lista o filtro.
- **Cheque de EMC**: o `/peqol withdraw` transforma EMC em um item que pode ser trocado entre jogadores. Clique com o botão direito para resgatar; agachado, você resgata a pilha inteira. O cheque não tem valor de EMC, então não dá para queimá-lo e duplicar EMC.
- **Pergaminho do Conhecimento**: grava tudo o que você aprendeu. Outro jogador clica com o botão direito e aprende tudo o que está gravado, ótimo para servidores. Agachar e clicar com o botão direito apaga o pergaminho.

### Blocos (ficam vinculados a quem os coloca)
- **Conexão Alquímica**: recebe EMC de Coletores e Relés e manda direto para o seu EMC pessoal. Enquanto você está offline, guarda o EMC num buffer.
- **Importador Alquímico**: uma "mesa de transmutação" para automação. Itens inseridos por funis ou canos viram seu EMC e são aprendidos. Itens sem EMC ficam na origem. Se você estiver offline, guarda o EMC até você entrar.
- **Exportador Alquímico**: clique nele com um item para filtrar. Ele cria os itens filtrados com o seu EMC e os empurra para os inventários vizinhos. Um sinal de redstone pausa o bloco.

Clicar com a mão vazia mostra dono, estado e estatísticas no chat.

### Comandos `/peqol`
| Comando | Descrição |
|---|---|
| `balance [jogador]` | Mostra o EMC (ver o de outro jogador exige OP) |
| `knowledge [jogador]` | Mostra quantos itens o jogador aprendeu |
| `pay <jogador> <quantia>` | Envia EMC para outro jogador. Aceita `1.5k`, `20M`, `3B`... |
| `withdraw <quantia> [cheques]` | Saca o EMC em Cheques de EMC |
| `worth [hand\|inventory]` | Mostra quanto vale a mão ou o inventário |
| `burn hand\|inventory` | Queima a mão ou o inventário principal. A hotbar, a armadura, a mão secundária, os itens com NBT e as Estrelas de Klein são preservados |
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

## Configuração
- `config/peqol-common.toml`: permite desativar `pay`, `withdraw`, `burn` e a queima rápida, e definir se é preciso ter a Tábua. Também controla o intervalo e o limite do Reabastecedor, o buffer da Conexão, a velocidade do Exportador, o aprender ao coletar (desligado por padrão) e outras opções.
- `config/peqol-client.toml`: HUD e tooltips.

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
- `/peqol` commands: balance, pay, withdraw, worth, burn, missing, top, knowledge.

Fully translated to English and Brazilian Portuguese.
