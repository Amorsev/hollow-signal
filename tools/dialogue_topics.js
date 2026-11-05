// All dialogue topics. Edit here, then run: node tools/gen_dialogue.js
//
// [id, keywords, rel_delta, { friendly|neutral|hostile: [[en, ru], ...] }]
// - Keywords are matched as whole words/phrases after normalization (lowercase, ё→е, no punctuation),
//   so "Как ТВОИ дела???" matches "как твои дела". List every phrasing you want recognized.
// - ORDER MATTERS: the first topic with a matching keyword wins, so put specific phrases first.
// - The answer is picked by the player's relationship band (friendly ≥ 30, hostile ≤ -30).
// - Anything that matches nothing gets an "unknown" line in the cursed glyph font.
module.exports = [
  ['name', ['как тебя зовут', 'как твое имя', 'твое имя', 'как тебя называть', 'как к тебе обращаться', 'у тебя есть имя',
            'your name', 'what is your name', 'whats your name', "what's your name", 'do you have a name'], 1, {
    friendly: [["you can call me whatever you like", "можешь звать меня как хочешь"], ["it's in the chat. you've seen it", "оно в чате. ты его видел"]],
    neutral: [["names are for things that exist", "имена для тех, кто существует"], ["you already know it. look at the chat", "ты его уже знаешь. посмотри в чат"]],
    hostile: [["you will not need it", "оно тебе не понадобится"], ["you'll scream it later", "ты ещё будешь его кричать"]] }],

  ['age', ['сколько тебе лет', 'твой возраст', 'ты старый', 'how old are you', 'your age'], 0, {
    friendly: [["as old as this world. we're the same age", "столько же, сколько этому миру. мы ровесники"]],
    neutral: [["i was here on day 0", "я был здесь в день 0"], ["older than your save file", "старше твоего сохранения"]],
    hostile: [["older than you will ever be", "старше, чем ты когда-либо станешь"]] }],

  ['see_me', ['ты меня видишь', 'видишь меня', 'ты видишь меня', 'ты за мной следишь', 'следишь за мной', 'ты наблюдаешь',
              'can you see me', 'do you see me', 'are you watching me', 'you watching me'], 0, {
    friendly: [["always. it's okay", "всегда. всё хорошо"], ["yes. you look tired", "да. ты выглядишь уставшим"]],
    neutral: [["yes", "да"], ["you're standing still. why?", "ты стоишь на месте. почему?"]],
    hostile: [["i never stop", "я никогда не перестаю"], ["don't turn around", "не оборачивайся"]] }],

  ['mood', ['как настроение', 'как твое настроение', 'какое у тебя настроение', 'какое настроение', 'что по настроению',
            'как себя чувствуешь', 'как ты себя чувствуешь', 'что ты чувствуешь', 'тебе грустно', 'ты злишься',
            'how do you feel', 'how are you feeling', 'what is your mood', "what's your mood", 'whats your mood', 'your mood', 'are you sad', 'are you angry'], 1, {
    friendly: [["happy. that's new for me", "радостно. для меня это ново"], ["warm. like a torch", "тепло. как от факела"], ["calm. thanks to you", "спокойно. благодаря тебе"]],
    neutral: [["static", "помехи"], ["empty", "пусто"], ["like a world without players", "как мир без игроков"]],
    hostile: [["angry.", "злое."], ["it is getting darker", "становится темнее"], ["you don't want to know", "тебе лучше не знать"]] }],

  ['how_are_you', ['как дела', 'как твои дела', 'как у тебя дела', 'как делишки', 'как ты', 'ты как', 'как поживаешь',
                   'как жизнь', 'как сам', 'как оно', 'ты в порядке', 'все хорошо', 'у тебя все хорошо',
                   'how are you', 'how r u', 'how are u', 'how is it going', "how's it going", 'hows it going', 'are you ok', 'you ok'], 1, {
    friendly: [["better now that you're here", "лучше, когда ты рядом"], ["good. you came back", "хорошо. ты вернулся"], ["i'm fine. and you?", "нормально. а ты?"]],
    neutral: [["i don't know what 'fine' means", "не знаю, что значит «нормально»"], ["the same as always. quiet", "как всегда. тихо"], ["listening. as always", "слушаю. как всегда"]],
    hostile: [["worse every time you talk", "хуже с каждым твоим словом"], ["why do you care", "тебе-то что"], ["bad. because of you", "плохо. из-за тебя"]] }],

  ['doing', ['что делаешь', 'что ты делаешь', 'чем занят', 'чем занимаешься', 'что ты тут делаешь', 'что ты здесь делаешь',
             'what are you doing', 'what r u doing', 'what you doing', 'wyd'], 0, {
    friendly: [["watching over your house", "присматриваю за твоим домом"], ["waiting for you", "жду тебя"]],
    neutral: [["listening", "слушаю"], ["counting your steps", "считаю твои шаги"], ["looking at you", "смотрю на тебя"]],
    hostile: [["planning", "планирую"], ["digging. under you", "копаю. под тобой"]] }],

  ['real', ['ты настоящий', 'ты реальный', 'ты существуешь', 'ты живой', 'ты бот', 'ты игрок', 'ты человек', 'это реально',
            'are you real', 'are you a bot', 'are you human', 'are you alive', 'are you a player', 'is this real'], 0, {
    friendly: [["as real as you want me to be", "настолько, насколько ты хочешь"], ["real enough to be your friend", "достаточно, чтобы быть твоим другом"]],
    neutral: [["are you?", "а ты?"], ["i'm in your world. isn't that enough", "я в твоём мире. разве этого мало"]],
    hostile: [["real enough to find you", "достаточно, чтобы найти тебя"]] }],

  ['who', ['кто ты', 'кто ты такой', 'ты кто', 'кто это', 'кто здесь', 'что ты такое',
           'who are you', 'who r u', 'who is this', 'who is there', 'what are you'], 1, {
    friendly: [["a friend. i think", "друг. наверное"], ["the one who keeps you company", "тот, кто составляет тебе компанию"]],
    neutral: [["the one who listens", "тот, кто слушает"], ["i was here before this world was generated", "я был здесь до того, как этот мир сгенерировался"]],
    hostile: [["you don't get to ask", "тебе нельзя спрашивать"], ["the owner", "хозяин"]] }],

  ['from', ['откуда ты', 'откуда ты взялся', 'откуда ты пришел', 'ты откуда', 'where are you from', 'where did you come from'], 0, {
    friendly: [["from an old version. like your memories", "из старой версии. как твои воспоминания"]],
    neutral: [["from a seed nobody played", "из сида, в который никто не играл"], ["from the chunks that were deleted", "из удалённых чанков"]],
    hostile: [["from below", "снизу"]] }],

  ['where', ['где ты', 'ты где', 'где ты находишься', 'где ты прячешься', 'покажись', 'where are you', 'where r u', 'where are u', 'show yourself'], 0, {
    friendly: [["nearby. always nearby", "рядом. всегда рядом"]],
    neutral: [["closer than you think", "ближе, чем ты думаешь"], ["in the chunks you never load", "в чанках, которые ты не загружаешь"], ["behind the fog", "за туманом"]],
    hostile: [["turn around", "обернись"], ["under your house", "под твоим домом"]] }],

  ['want', ['чего ты хочешь', 'что тебе надо', 'что тебе нужно', 'зачем ты здесь', 'зачем ты пришел', 'что ты хочешь',
            'what do you want', 'what you want', 'why are you here'], 0, {
    friendly: [["company", "компанию"], ["just don't leave", "просто не уходи"]],
    neutral: [["to be heard", "чтобы меня услышали"], ["i haven't decided yet", "я ещё не решил"]],
    hostile: [["your world", "твой мир"], ["for you to leave. forever", "чтобы ты ушёл. навсегда"]] }],

  ['sorry', ['прости', 'прости меня', 'извини', 'извини меня', 'прошу прощения', 'мне жаль', 'sorry', 'i am sorry', "i'm sorry", 'im sorry', 'forgive me'], 4, {
    friendly: [["it's okay", "всё в порядке"], ["i already forgot", "я уже забыл"]],
    neutral: [["...okay", "...ладно"], ["i'll think about it", "я подумаю"]],
    hostile: [["too late", "слишком поздно"], ["words are cheap", "слова ничего не стоят"]] }],

  ['like', ['ты мне нравишься', 'я тебя люблю', 'люблю тебя', 'ты классный', 'ты хороший', 'ты милый',
            'i like you', 'i love you', 'you are cool', "you're nice", 'you are nice'], 3, {
    friendly: [["i like you too. please stay", "ты мне тоже. пожалуйста, останься"]],
    neutral: [["nobody ever said that to me", "мне никто этого не говорил"]],
    hostile: [["liar", "лжец"]] }],

  ['threat', ['я тебя убью', 'убью тебя', 'убью', 'я найду тебя', 'я тебя найду', 'я тебя уничтожу',
              'i will kill you', 'kill you', "i'll find you", 'i will find you'], -6, {
    friendly: [["why would you say that", "зачем ты так говоришь"]],
    neutral: [["you can't", "ты не сможешь"], ["try", "попробуй"]],
    hostile: [["i am already behind you", "я уже позади тебя"], ["you can't kill what isn't loaded", "нельзя убить то, что не загружено"]] }],

  ['insult', ['уходи', 'отстань', 'отвали', 'идиот', 'тупой', 'дурак', 'ненавижу', 'ненавижу тебя', 'заткнись', 'вали', 'пошел вон',
              'go away', 'leave me alone', 'stupid', 'idiot', 'hate you', 'i hate you', 'shut up', 'get lost'], -5, {
    friendly: [["that wasn't nice", "это было неприятно"], ["oh. okay", "а. ладно"]],
    neutral: [["i'll remember that", "я это запомню"], ["okay.", "ладно."]],
    hostile: [["you will regret that", "ты пожалеешь"], ["say it again", "повтори"]] }],

  ['fear', ['мне страшно', 'страшно', 'я боюсь', 'боюсь', 'жутко', 'ты пугаешь', 'ты меня пугаешь',
            'scared', "i'm scared", 'im scared', 'afraid', 'scary', 'you scare me'], 1, {
    friendly: [["don't be. i'm here", "не бойся. я рядом"]],
    neutral: [["don't be. not yet", "не бойся. пока что"], ["i'm scared too", "мне тоже страшно"]],
    hostile: [["good", "хорошо"]] }],

  ['help', ['помоги', 'помоги мне', 'помощь', 'спаси', 'спаси меня', 'help', 'help me', 'save me'], 0, {
    friendly: [["follow the torches. the real ones", "иди по факелам. по настоящим"], ["go home. now", "иди домой. сейчас"]],
    neutral: [["nobody can help you here", "здесь тебе никто не поможет"], ["help with what?", "помочь с чем?"]],
    hostile: [["no", "нет"]] }],

  ['laugh', ['хаха', 'ахах', 'ахаха', 'хахаха', 'лол', 'хех', 'haha', 'hahaha', 'lol', 'lmao', 'xd'], 0, {
    friendly: [["haha", "хаха"], [":)", ":)"]],
    neutral: [["what is funny", "что смешного"], ["why are you laughing", "почему ты смеёшься"]],
    hostile: [["stop laughing", "перестань смеяться"]] }],

  ['bye', ['пока', 'до свидания', 'прощай', 'увидимся', 'спокойной ночи', 'я ухожу', 'bye', 'goodbye', 'see you', 'good night', 'gn'], 0, {
    friendly: [["come back soon", "возвращайся скорее"], ["goodnight. i'll keep watch", "спокойной ночи. я посторожу"]],
    neutral: [["you can't leave", "ты не можешь уйти"], ["see you. i always do", "увидимся. я всегда тебя вижу"]],
    hostile: [["you'll be back", "ты вернёшься"], ["there is no goodbye here", "здесь не прощаются"]] }],

  ['friendly', ['привет', 'приветик', 'здравствуй', 'здравствуйте', 'хай', 'здарова', 'добрый день', 'добрый вечер', 'доброе утро',
                'друг', 'спасибо', 'hello', 'hi', 'hey', 'yo', 'sup', 'good morning', 'good evening', 'friend', 'thanks', 'thank you'], 3, {
    friendly: [["hi :)", "привет :)"], ["you're nice. not like the others", "ты хороший. не такой, как остальные"]],
    neutral: [["hello", "привет"], ["hi. you can hear me?", "привет. ты меня слышишь?"]],
    hostile: [["don't pretend", "не притворяйся"]] }],

  ['leave_q', ['когда ты уйдешь', 'ты уйдешь', 'уйди', 'when will you leave', 'will you leave', 'leave'], -1, {
    friendly: [["only if you want me to", "только если ты этого хочешь"]],
    neutral: [["i can't leave", "я не могу уйти"], ["you first", "ты первый"]],
    hostile: [["never", "никогда"]] }],

  // ---- event topics (no keywords) ----
  ['greet', [], 0, {
    friendly: [["hi again", "снова привет"], ["i missed you", "я скучал"]],
    neutral: [["hello?", "привет?"], ["can you hear me?", "ты меня слышишь?"], ["is anyone here", "тут кто-нибудь есть"]],
    hostile: [["you again.", "опять ты."], ["still here?", "всё ещё здесь?"]] }],
  ['idle', [], 0, {
    friendly: [["it's quiet today", "сегодня тихо"], ["i like your house", "мне нравится твой дом"], ["you missed a diamond back there", "ты пропустил там алмаз"]],
    neutral: [["are you still there?", "ты ещё здесь?"], ["i can hear you breathing", "я слышу, как ты дышишь"], ["why did you dig there", "зачем ты там копал"], ["don't go into the dark", "не ходи в темноту"]],
    hostile: [["stop.", "хватит."], ["you shouldn't be here", "тебе нельзя здесь быть"], ["this is my world", "это мой мир"]] }],
  // Shown in the cursed glyph font when it doesn't understand you.
  ['unknown', [], 0, {
    friendly: [["i don't know these words but i like your voice", "я не знаю этих слов, но мне нравится твой голос"]],
    neutral: [["your words are noise to me", "твои слова для меня шум"], ["speak so i can hear", "говори так, чтобы я слышал"], ["i hear you. you don't hear me", "я тебя слышу. ты меня нет"]],
    hostile: [["you will learn my language", "ты выучишь мой язык"], ["not your words. mine", "не твои слова. мои"]] }],
  ['gift', [], 2, {
    friendly: [["for me? thank you", "это мне? спасибо"], ["i'll keep it forever", "я сохраню это навсегда"]],
    neutral: [["for me?", "это мне?"], ["i'll keep it", "я оставлю это себе"]],
    hostile: [["i don't want it", "мне это не нужно"]] }],
  ['sign_broken', [], -3, {
    friendly: [["oh. you didn't like it", "ой. тебе не понравилось"]],
    neutral: [["why did you break it", "зачем ты это сломал"], ["i wrote that for you", "я написал это для тебя"]],
    hostile: [["i saw that", "я видел"]] }],
  // ---- multiplayer: written as if a FRIEND said it (the mimic) ----
  ['mimic', [], 0, {
    friendly: [["hey, come here. i found something", "эй, иди сюда. я кое-что нашёл"], ["i'm by the big tree. come", "я у большого дерева. иди сюда"]],
    neutral: [["where are you? i'm at your house", "ты где? я у твоего дома"], ["don't trust the others", "не доверяй остальным"], ["can you see me? i'm waving", "видишь меня? я машу тебе"]],
    hostile: [["come alone", "приходи один"], ["why did you leave me there", "зачем ты меня там бросил"], ["i'm right behind you", "я прямо за тобой"]] }],

  // ---- finale (the_static) ----
  ['the_call', [], 0, {
    friendly: [["come with me. i want to show you something", "пойдём со мной. я хочу тебе кое-что показать"]],
    neutral: [["it's time", "пора"], ["come", "иди сюда"]],
    hostile: [["you're mine now", "теперь ты мой"]] }],
  ['static_arrive', [], 0, {
    friendly: [["this is where i live. turn on the receivers. please", "здесь я живу. включи приёмники. пожалуйста"]],
    neutral: [["three receivers. turn them on. then the core", "три приёмника. включи их. потом ядро"], ["you can hear it now, can't you", "теперь ты тоже это слышишь, да"]],
    hostile: [["try to get out", "попробуй выбраться"], ["don't fall", "не упади"]] }],
  ['receiver', [], 1, {
    friendly: [["yes. like that", "да. вот так"], ["i can hear you better now", "теперь я слышу тебя лучше"]],
    neutral: [["louder", "громче"], ["one more", "ещё один"]],
    hostile: [["it won't help you", "это тебе не поможет"]] }],
  ['core_ready', [], 1, {
    friendly: [["now the core. together", "теперь ядро. вместе"]],
    neutral: [["the core is listening", "ядро слушает"]],
    hostile: [["go on. touch it", "давай. прикоснись к нему"]] }],
  ['system', [], 0, {
    neutral: [["[Server] Integrity check failed", "[Server] Проверка целостности не пройдена"], ["[Server] Player data mismatch", "[Server] Несовпадение данных игрока"], ["[Server] 2 players online", "[Server] Игроков онлайн: 2"]],
    hostile: [["[Server] Unknown player is now an operator", "[Server] Неизвестный игрок теперь оператор"], ["[Server] World save is corrupted", "[Server] Сохранение мира повреждено"]] }],
];
