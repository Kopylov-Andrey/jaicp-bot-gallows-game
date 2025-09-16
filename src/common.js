function onlyLetters(s) { 
    return (s || '').toLowerCase().replace(/ё/g, 'е').replace(/[^а-яa-z]+/g, ''); 
}

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