var banishKey = "ct_banish_key";
var isEnabled = true;

function getBanishIndex() {
    return localStorage.getItem(banishKey) || 0;
}

function getItemCard(i) {
    return document.querySelectorAll(".result-row:not(.banished)")[i];
}

function setCardBackground(elem) {
    elem.style.backgroundColor = "antiquewhite";
}

function removeCardBackground(elem) {
    elem.style.backgroundColor = "";
}

function setBanishIndex(i) {
    var curIdx = getBanishIndex();
    localStorage.setItem(banishKey, i);
    setCardBackground(getItemCard(i));
    if (i != curIdx) {
        removeCardBackground(getItemCard(curIdx));
    }
}

function banishItem(i) {
    getItemCard(i).querySelector(".banish").click();
}


// keyboard shortcuts
function onKeyDown(e) {
    var nodeName = e.target.nodeName;
    if (nodeName == "INPUT" || nodeName == "TEXTAREA") {
        return;
    }

    var keyCode = e.keyCode;

    if (e.ctrlKey && e.shiftKey) {
        switch (keyCode) {
            // e key
            case 69:
                isEnabled = true;
                console.log("Enable Craigslist Tools!");
                break;
            // d key
            case 68:
                isEnabled = false;
                console.log("Disable Craigslist Tools!");
                break;
        }
    }

    if (isEnabled) {
        switch (e.keyCode) {
            // del
            case 46:
                banishItem(getBanishIndex());
                setBanishIndex(getBanishIndex());
                break;
            // left arrow
            case 37:
                curIdx = getBanishIndex();
                if (curIdx != 0) {
                    setBanishIndex(--curIdx);
                }
                break;
            // right arrow
            case 39:
                curIdx = getBanishIndex();
                setBanishIndex(++curIdx);
                break;
        }
    }
}

setBanishIndex(getBanishIndex());

document.addEventListener("keydown", onKeyDown, false);