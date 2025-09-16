require: slotfilling/slotFilling.sc
    module = sys.zb-common
  
require: text/text.sc
    module = sys.zb-common

require: common.js
    module = sys.zb-common
    
# Для игры Назови столицу    
require: where/where.sc
    module = sys.zb-common

# Для игры Виселица
require: hangmanGameData.csv
    name = HangmanGameData
    var = $HangmanGameData
    


patterns:
    $Word = $entity<HangmanGameData> || converter = function ($parseTree) {
        var id = $parseTree.HangmanGameData[0].value;
        return $HangmanGameData[id].value;
        };
    $liter = (а|б|в|г|д|е|ё|ж|з|и|й|к|л|м|н|о|п|р|с|т|у|ф|х|ц|ч|ш|щ|ъ|ы|ь|э|ю|я)
    $Yes = (да|хорошо|давай|напомни|Приступим)
    $No = (Нет|Не хочу)
   

theme: /

    state: Start
        q!: $regex</start>
        intent: /привет
        script:
            $jsapi.startSession();
        a: Эй, хочешь поиграть? Урок всё равно скучный…
        
        
    state: End
        script:
            $session.hm = null;
       

    state: No
        q: * $No *
        a: Ну ё-моё… жаль! Если вдруг передумаешь — просто пиши «Давай поиграем». Я тут.
        go!:/End
        
    
        
    state: Hangman
        # a: Hangman
        intent!: /Давай поиграем
        q: * $Yes *
        a: Йес! Ща всё устроим и сыграем в «Виселицу». Хочешь напомню, как играть?
        
        
        state: CatchAll || noContext=true
            # a: Hangman CatchAll
            event: noMatch || fromState = "/Hangman", onlyThisState = true
            random:
                a: Эээ, чё-то не догнал. Надо объяснить, как играть в «Виселицу»?
                a: Подожди, не понял. Тебе напомнить, как играть?
        
        state: Rules
            # a: Hangman Rules
            q: * $Yes *
            a: Всё просто: я загадываю слово, а ты по одной букве (или сразу всё) пытаешься отгадать. Попал — круто, не попал — пиши ошибку в дневник 😄
            a: Только не косячь, а то после 6 ошибок игра вылетает в окно 😬
            a: И ещё! Чтобы не путаться — можешь в любой момент спросить: «Какие буквы я уже пробовал?». Ну чё, погнали?
            state: Yes
                # a: Hangman Rules Yes
                q: * $Yes *
                script:
                    $reactions.transition("/Hangman/Play");
                
            state: No
                # a: Hangman Rules  No
                q: * $No *
                a: Эээх, обидно слышать. Но если вдруг надумаешь — просто пиши «Давай поиграем».
                script:
                    $reactions.transition("/End");
        
            
            
        state: Start
            # a: Hangman Start
            q: $regexp_i<(Нет)>
            a: Ну чё, без лишних слов — сразу в бой!
            go!: /Hangman/Play
            
            
        state: Play
            # a: Hangman Play
            q!: $regexp_i<$liter|$Word>
            script:
                
                function norm(s) { return (s || '').toLowerCase().replace(/ё/g, 'е'); }
                function onlyLetters(s) { return (s || '').toLowerCase().replace(/ё/g, 'е').replace(/[^а-яa-z]+/g, ''); }
                function buildMask(word, opened, letter) {
                    var mask = word.split('').map(function (ch) {
                        var displayChar = opened.indexOf(ch) >= 0 ? ch : '_';
                        if (letter && ch === letter) {
                            displayChar = displayChar.toUpperCase();
                        }
                        return displayChar;
                    }).join(' ');
                
                    return mask;
                }
                
        
                var qText = $request.query || '';
                var text = onlyLetters(qText);
                var attempts = $session.hm ? $session.hm.attempts - $session.hm.numErrors : 6; // Подсчитываем оставшиеся попытки
                
                
                    
                if (!$session || !$session.hm || !$session.hm.word) {
                    // Случайный выбор слова из HangmanGameData
                    
                    var list = $HangmanGameData;
                    var i = Math.floor(Math.random() * (list.length || 1));
                    var rec = list[i] || {};
                    var raw = (rec.value.word).toString().trim();
                    word = norm(raw);
                    if($session.test !== undefined){word =$session.test;}
                    $session.len_word = word.length
                    if (!$session.guess) {
                        $session.guess = 'парта'; // Запасной вариант, если в датасете пусто
                    }
        
                    // Инициализация сессии
                    $session.hm = { word: word, attempts: 6, opened: [], tried: [], numErrors: 0 };
                    $session.guess = word[0].toUpperCase() + word.substr(1);
                    
                    $session.numErrors = 0;
                    
                    // Маска слова
                    var mask = word.split('').map(function () { return '_'; }).join(' ');
                    if (Math.random() > 0.5) {
                        $temp.answer = 'Слово уже в голове! Вот, смотри: \n' + mask + '\n(' + $session.guess + ')';}
                    else {
                        $temp.answer = 'Загадал! Смотри, как выглядит: \n' + mask + '\n(' + $session.guess + ')';}
                    
                    $reactions.answer($temp.answer);
                    $reactions.answer('Твоя очередь, дружище! Кидай букву или слово целиком.')
                    return;
                    
                } 
                else {
                    
                    var sayStop = ['хватит', 'надоело', 'стоп', 'сдаюсь'];
                    if (sayStop.indexOf(text.trim().toLowerCase()) !== -1) {
                        $reactions.answer('Эх, жаль! Но ты всегда можешь вернуться — просто пиши "Давай поиграем".');
                        $session.hm = null;
                        $reactions.transition("/End");
                        return;
                    }
                    
                     
                    if(text === 'какиебуквыяужепробовал'){
                        var triedLetters = $session.hm.tried.join(', ');
                        $reactions.answer('Уже были такие буквы: ' + triedLetters);
                    }
                    
        
                    // Ввод пользователя
                    if (!text) {
                        $temp.answer = 'А? Не расслышал! Назови букву или слово — не стесняйся.';// -----------------------------------------------------------------
                        $reactions.answer($temp.answer);
                        return;
                    }
                    if (text === 'подсказка') {
                        var word = $session.guess;
                        var opened = $session.hm.opened;
                        var hintLetter = null;
                    
                        for (var i = 0; i < word.length; i++) {
                            var ch = word[i];
                            if (opened.indexOf(ch) < 0) {
                                hintLetter = ch;
                                break;
                            }
                        }
                    
                        if (hintLetter) {
                            $session.hm.opened.push(hintLetter);
                            $session.hm.tried.push(hintLetter);
                            $session.numErrors += 1; // Подсказка стоит одну попытку
                    
                            // Проверка на победу
                            var openedObj = {};
                            $session.hm.opened.forEach(function(ch) { openedObj[ch] = true; });
                    
                            var isWin = true;
                            word.split('').forEach(function(ch) {
                                if (!openedObj[ch]) isWin = false;
                            });
                    
                            if (isWin) {
                                $reactions.answer('Лови подсказку! Эта буква была: "' + hintLetter + '".');
                                $reactions.answer('Вау! Ты всё слово угадал! Это оно: ' + word + ' 🎉');
                                 $reactions.transition("/End");
                                $session.hm = null;
                            } else {
                                var mask = buildMask(word, $session.hm.opened, hintLetter);
                                $reactions.answer('Лови подсказку! Я раскрыл тебе букву: "' + hintLetter.toUpperCase() + '".');
                                $reactions.answer(mask + '\nОсталось всего попыток: ' + (6 - $session.numErrors));
                            }
                        } else {
                            $reactions.answer('Больше не могу подсказывать — все буквы и так уже на столе 🤷');
                        }
                        return;
                    }
                    if ($request.query.length == 1 || $request.query.length == $session.len_word){
                        
                        if (text.length > 1) {                           // Пользователь ввел слово
                            if (text !== $session.guess.toLowerCase()) {
                                $session.numErrors == 6;
                                $temp.answer = 'Эээ… ну ты балбес! Это вообще не то.';
                                $reactions.answer($temp.answer);
                                $temp.answer = 'Я загадал слово: ' + $session.guess;
                                $reactions.answer($temp.answer);
                                $reactions.answer('Хочешь ещё раунд? Просто напиши "Давай поиграем"!');
                                $session.hm = null;
                                $reactions.transition("/End");
                            } 
                            else {
                                $reactions.answer('Еееемаа, легенда! Ты справился 🎉!');
                                $reactions.answer($session.guess);
                                $reactions.answer('Хочешь ещё раунд? Просто напиши "Давай поиграем"!');
                                $session.hm = null; // Очистить сессию
                                $reactions.transition("/End");
                                
                            }
                        } 
                        else {
                            // Пользователь ввел букву
                            var letter = text[0];
            
                            // Проверка на уже использованные буквы
                            if ($session.hm.tried.indexOf(letter) >= 0) {
                                $reactions.answer('Эй, ты же уже говорил  букву «' + letter + '» — не повторяйся 😅.');
                                $reactions.answer(buildMask($session.guess.toLowerCase(), $session.hm.opened));
                                return;
                            }
            
                            // Добавляем букву в список пробованных
                            $session.hm.tried.push(letter);
            
                            // Проверка, есть ли буква в слове
                            if ($session.guess.toLowerCase().indexOf(letter) >= 0) {
                                
                                $session.hm.opened.push(letter);
                                
                                
                                var openedObj = {};
                                var uniqueLettersInWord = $session.guess.toLowerCase().split('');
                            
                                // Заполняем объект с уникальными буквами из opened
                                $session.hm.opened.forEach(function(ch) {
                                    openedObj[ch] = true;
                                });
                                
                                var isWin = true;
                                uniqueLettersInWord.forEach(function(ch) {
                                    if (!openedObj[ch]) {
                                        isWin = false;
                                    }
                                });
                                
                                
                                if (isWin) {
                                    $reactions.answer('Ну это было мощно! Слово угадано, поздравляю ✌️');
                                    $reactions.answer($session.guess);
                                    $reactions.answer('Хочешь ещё раунд? Просто напиши "Давай поиграем"!');
                                     $session.hm = null; 
                                    $reactions.transition("/End");
                                } 
                                
                                else $reactions.answer(buildMask($session.guess.toLowerCase(), $session.hm.opened, letter));
                                                            
                                
                                
                            } 
                            
                            
                            else {
                                $session.numErrors += 1;
                                if ($session.numErrors !== 6) {
                                    $reactions.answer('А вот этой буквы в слове точно нет 😬');
                                    $reactions.answer('Осталось попыток: ' + (6 - $session.numErrors) + '.');
                                    $reactions.answer(buildMask($session.guess.toLowerCase(), $session.hm.opened));
                                }
                                else{
                                    $reactions.answer('Эх, не повезло... Я загадывал: "' + $session.guess + '".');
                                    $reactions.answer('Если захочешь взять реванш — просто пиши «Давай поиграем».');
                                    $session.hm = null; // Очистить сессию для новой игры
                                    $reactions.transition("/End");
                                    return;
                                }
                            }
                            if ($session.hm !== null)
                            {
                                var sayRandom = ['Ну что, твой ход! Пуляй букву или слово.', 'Что дальше? Какой вариант кинем в бой?', 'Давай подумаем... какую букву проверим теперь?	'];
                                var randomIndex = Math.floor(Math.random() * 3);
                                $reactions.answer(sayRandom[randomIndex]);
                            }
                        }
                    }
                }
                
                
            
            state: CatchAll || noContext=true
            # a: Hangman Play CatchAll
            event: noMatch || fromState = "/Hangman/Play", onlyThisState = true
            script:
                function onlyLetters(s) { return (s || '').toLowerCase().replace(/ё/g, 'е').replace(/[^а-яa-z]+/g, ''); }
                var query = $request.query.trim().toLowerCase();
                var list = ['да', 'нет', 'хорошо', 'напомни','давай','нехочу','приступим', 'подсказка', 'какиебуквыяужепробовал']
                if(query.length == 1 || query.length == $session.len_word ||list.indexOf(onlyLetters(query)) !== -1){return;}
                else{
                    if (Math.random() > 0.5) {
                        $reactions.answer('Слушай, я тебя не понял… Скажи букву или слово, а?');
                    }
                    else {
                        $reactions.answer('Эээм… не улавливаю. Назови букву или всё слово.');
                    }
                    
                }
                
        

    state: NoMatch || noContext=true
        event!: noMatch
        script:
            if ($session && $session.hm && $session.hm.word) {
                $reactions.transition('/Hangman/Play');
                return;
            }
        random:
            a: Эй, повтори плиз — я сбился 😅
            a: Это что было? 😄
            a: Упс, не догнал. Повтори?
       

    # state: reset
    #     q!: reset
    #     script:
    #         $session = {};
    #         $client = {};
    #     go!: /Start
