import java.util.ArrayList;

public class Bot {

    public int adr;
    public int x;
    public int y;
    public int health;
    public int mineral;
    public int alive;
    public int c_red;
    public int c_green;
    public int c_blue;
    public int direction;
    public Bot mprev;
    public Bot mnext;

    public int[] mind = new int[64];                // геном бота содержит 64 команды


    public int MIND_SIZE = 64; //Объем генома
    //===================          BOT.LIVING                 ======================
    //======= состяние бота, которое отмеченно для каждого бота в массиве bots[] ====================
    public int LV_FREE = 0;  // место свободно, здесь может быть размещен новый бот
    public int LV_ORGANIC_HOLD = 1;  // бот погиб и представляет из себя органику в подвешенном состоянии
    public int LV_ORGANIC_SINK = 2;  // ораника начинает тонуть, пока не встретит препятствие, после чего остается в подвешенном состоянии(LV_ORGANIC_HOLD)
    public int LV_ALIVE = 3;  // живой бот


    public Bot() {
        direction = 2;
        health = 5;
        alive = 3;
    }



    // ====================================================================
    // =========== главная функция жизнедеятельности бота  ================
    // =========== в ней выполняется код его мозга-генома  ================
    // ====================================================================
    public void step() {
        if (alive == 0 || alive == 1 || alive == 2) return;   //Это труп - выходим!

        for (int cyc = 0; cyc < 15; cyc++) {
            int command = mind[adr];  // текущая команда
//...............  сменить направление относительно   ....
            if (command == 23) {                            // вычисляем новое направление
                int param = botGetParam(this) % 8;          // берём следующи байт за командой и вычисляем остаток от деления на 8
                int newdrct = direction + param;            // полученное число прибавляем к значению направления бота
                if (newdrct >= 8) {
                    newdrct = newdrct - 8;
                }// результат должен быть в пределах от 0 до 8
                direction = newdrct;
                botIncCommandAddress(this, 2);                              // адрес текущей команды увеличивается на 2,
            }
//...............  сменить направление абсолютно   ....
            if (command == 24) {                // записываем новое значение направления
                direction = botGetParam(this) % 8;  // берем следующий байт и вычисляем остаток от деления на 8
                botIncCommandAddress(this, 2);                  // адрес текущей команды увеличивается на 2,
            }
//...............  фотосинтез ................
            if (command == 25) {
                botEatSun(this);                            // выполняем команду фотосинтеза
                botIncCommandAddress(this, 1);                              // адрес текущей команды увеличивается на 1
                break;         // выходим, так как команда шагнуть - завершающая
            }

//...............  шаг  в относительном напралении  .................
            if (command == 26) {
                if (isMulti(this) == 0) {           // бот многоклеточный? перемещаются только одноклеточные
                    int drct = botGetParam(this) % 8;   // вычисляем направление из следующего за командой байта
                    botIndirectIncCmdAddress(this, botMove(this, drct, 0)); // меняем адрес текущей команды
                    // в зависимости от того, что было в этом направлении
                    // смещение условного перехода 2-пусто  3-стена  4-органика 5-бот 6-родня
                } else {
                    botIncCommandAddress(this, 2);
                }
                break;         // выходим, так как команда шагнуть - завершающая
            }
//...............  шаг   в абсолютном направлении     .................
            if (command == 27) {
                if (isMulti(this) == 0) {
                    int drct = botGetParam(this) % 8;
                    botIndirectIncCmdAddress(this, botMove(this, drct, 1));
                }
                break;
            }
//..............   съесть в относительном напралении       ...............
            if (command == 28) {
                int drct = botGetParam(this) % 8;       // вычисляем направление из следующего за командой байта
                botIndirectIncCmdAddress(this, botEat(this, drct, 0)); // меняем адрес текущей команды
                // в зависимости от того, что было в этом направлении
                //смещение условного перехода  стена - 2 пусто - 3 органика - 4 живой - 5
                break;  // выходим, так как команда шагнуть - завершающая
            }
//..............   съесть  в абсолютном направлении      ...............
            if (command == 29) {  //смещение условного перехода  стена - 2 пусто - 3 органика - 4 живой - 5
                int drct = botGetParam(this) % 8;
                botIndirectIncCmdAddress(this, botEat(this, drct, 1));
                break;
            }
//.............   посмотреть  в относительном напралении ...................................
            if (command == 30) {
                int drct = botGetParam(this) % 8;    // вычисляем направление из следующего за командой байта
                botIndirectIncCmdAddress(this, botSeeBots(this, drct, 0)); // меняем адрес текущей команды
                // в зависимости от того, что было в этом направлении
                // пусто - 2 стена - 3 органик - 4 бот -5 родня -  6
            }
//.............   посмотреть в абсолютном направлении ...................................
            if (command == 31)// пусто - 2 стена - 3 органик - 4 бот -5 родня -  6
            {
                int drct = botGetParam(this) % 8;
                botIndirectIncCmdAddress(this, botSeeBots(this, drct, 1));
            }

// делиться - если у бота больше энергии или минералов, чем у соседа, то они распределяются поровну
//.............   делится   в относительном напралении  ........................
            if ((command == 32) || (command == 42)) {   // здесь я увеличил шансы появления этой команды
                int drct = botGetParam(this) % 8;    // вычисляем направление из следующего за командой байта
                botIndirectIncCmdAddress(this, botCare(this, drct, 0)); // меняем адрес текущей команды
                // в зависимости от того, что было в этом направлении
                // стена - 2 пусто - 3 органика - 4 удачно - 5
            }
            //.............   делится  в абсолютном направлении ........................
            if ((command == 33) || (command == 50)) {    // здесь я увеличил шансы появления этой команды
                int drct = botGetParam(this) % 8;
                botIndirectIncCmdAddress(this, botCare(this, drct, 1));  // стена - 2 пусто - 3 органика - 4 удачно - 5
            }
// отдать - безвозмездно отдать часть энергии и минералов соседу
//.............   отдать   в относительном напралении  ........................
            if ((command == 34) || (command == 51)) {    // здесь я увеличил шансы появления этой команды
                int drct = botGetParam(this) % 8;    // вычисляем направление из следующего за командой байта
                botIndirectIncCmdAddress(this, botGive(this, drct, 0)); // меняем адрес текущей команды
                // в зависимости от того, что было в этом направлении
                // стена - 2 пусто - 3 органика - 4 удачно - 5
            }
//.............   отдать  в абсолютном направлении  ........................
            if ((command == 35) || (command == 52)) {      // здесь я увеличил шансы появления этой команды
                int drct = botGetParam(this) % 8;
                botIndirectIncCmdAddress(this, botGive(this, drct, 1)); // стена - 2 пусто - 3 органика - 4 удачно - 5
            }

//...................   выравнится по горизонтали  ...............................
            if (command == 36) {
                if (Math.random() < 0.5) {              // кидаем монетку
                    direction = 3;                 // если ноль, то поворачиваем в одну сторону
                } else {
                    direction = 7;                 // если один, то поворачиваем в другую сторону
                }
                botIncCommandAddress(this, 1); // увеличиваем указатель текущей команды на 1
            }
//...................  какой мой уровень (на какой высоте бот)  .........
            if (command == 37) {   // у меня поле высотой в 96 клеток
                // байт в геноме может иметь значение от 0 до 63
                // умножая значение байта на 1,5 получаем значение от 0 до 95
                int param = botGetParam(this) * World.simulation.height / MIND_SIZE;   // берем следующий за командой байт и умножаем на 1,5
                // если уровень бота ниже, чем полученное значение,
                // то прибавляем к указатели текущей команды значение 2-го байта, после выполняемой команды
                if (y < param) {
                    botIndirectIncCmdAddress(this, 2);
                } else { // иначе прибавляем к указатели текущей команды значение 3-го байта, после выполняемой команды
                    botIndirectIncCmdAddress(this, 3);
                }
            }
//...................  какое моё здоровье  ...............................
            if (command == 38) {   // максимальное здоровье  999
                // байт в геноме может иметь значение от 0 до 63
                // умножая значение байта на 15 получаем значение от 0 до 945
                int param = botGetParam(this) * 1000 / MIND_SIZE;   // берем следующий за командой байт и умножаем на 15
                // если здоровье бота ниже, чем полученное значение,
                // то прибавляем к указатели текущей команды значение 2-го байта, после выполняемой команды
                if (health < param) {
                    botIndirectIncCmdAddress(this, 2);
                } else { // иначе прибавляем к указатели текущей команды значение 3-го байта, после выполняемой команды
                    botIndirectIncCmdAddress(this, 3);
                }
            }
//...................сколько  минералов ...............................
            if (command == 39) {
                int param = botGetParam(this) * 1000 / MIND_SIZE;
                if (mineral < param) {
                    botIndirectIncCmdAddress(this, 2);
                } else {
                    botIndirectIncCmdAddress(this, 3);
                }
            }
//...........  многоклеточность ( создание потомка, приклееного к боту )......
            if (command == 40) {   // функция isMulti() возвращает
                // 0 - если бот не входит в многоклеточную цепочку
                // 1 или 2 - если бот является крайним в цепочке
                // 3 - если бот внутри цепочки
                int a = isMulti(this);    // 0 - нету, 1 - есть MPREV, 2 - есть MNEXT, 3 есть MPREV и MNEXT
                if (a == 3) {
                    botDouble(this);
                } else {    // если бот уже находится внутри цепочки, то новый бот рождается свободным
                    botMulti(this);     // в другом случае, новый бот рождается приклеенным к боту-предку
                }
                botIncCommandAddress(this, 1);   // увеличиваем адрес текущей команды на 1
                break; // выходим, так как команда родить - завершающая
            }
//...............  деление (создание свободноживущего потомка) ................
            if (command == 41) {
                int a = isMulti(this);
                if ((a == 0) || (a == 3)) {
                    botDouble(this);  // если бот свободный или внутри цепочки, , то новый бот рождается свободным
                } else {
                    botMulti(this);  // если бот крайний в цепочке, новый бот рождается приклеенным к боту-предку
                }
                botIncCommandAddress(this, 1);
                break;
            }
//...............  окружен ли бот    ................
            if (command == 43) {   // функция full_aroud() возвращает  1, если бот окружен и 2, если нет
                // увеличиваем значение указателя текущей команды
                // на значение следующего байта после команды или 2-го байта после команды
                // в зависимости от того, окружен бот или нет
                botIndirectIncCmdAddress(this, fullAroud(this));
            }
//.............. приход энергии есть? ........................
            if (command == 44) {  // is_health_grow() возвращает 1, если энегрия у бота прибавляется, иначе - 2
                botIndirectIncCmdAddress(this, isHealthGrow(this));
            }
//............... минералы прибавляются? ............................
            if (command == 45) {  // если глубина больше половины, то он начинает накапливать минералы
                if (y > (World.simulation.height / 2)) {
                    botIndirectIncCmdAddress(this, 1);
                } else {
                    botIndirectIncCmdAddress(this, 2);
                }
            }
//.............. многоклеточный ли я ? ........................
            if (command == 46) {
                int mu = isMulti(this);
                if (mu == 0) {
                    botIndirectIncCmdAddress(this, 1); // бот свободно живущий
                } else {
                    if (mu == 3) {
                        botIndirectIncCmdAddress(this, 3); // бот внутри цепочки
                    } else {
                        botIndirectIncCmdAddress(this, 2); // бот скраю цепочки
                    }
                }
            }
//.................. преобразовать минералы в энерию ...................
            if (command == 47) {
                botMineral2Energy(this);
                botIncCommandAddress(this, 1);
                break;      // выходим, так как команда - завершающая
            }
//................      мутировать   ...................................
// спорная команда, во время её выполнения меняются случайным образом две случайные команды
// читал, что микроорганизмы могут усилить вероятность мутации своего генома в неблагоприятных условиях
            if (command == 48) {
                int ma = (int) (Math.random() * 64);  // 0..63
                int mc = (int) (Math.random() * 64);  // 0..63
                mind[ma] = mc;

                ma = (int) (Math.random() * 64);  // 0..63
                mc = (int) (Math.random() * 64);  // 0..63
                mind[ma] = mc;
                botIncCommandAddress(this, 1);
                break;     // выходим, так как команда мутировать - завершающая
            }
//................   генная атака  ...................................
            if (command == 49) {  // бот атакует геном соседа, на которого он повернут
                botGenAttack(this); // случайным образом меняет один байт
                botIncCommandAddress(this, 1);
                break; // выходим, так как команда мутировать - завершающая
            }        // после её выполнения, управление передаётся следующему боту
        }
//................    если ни с одной команд не совпало .................
//................    значит безусловный переход        .................
//.....   прибавляем к указателю текущей команды значение команды   .....
        int command = mind[adr];  // текущая команда
        if (((command >= 0) && (command <= 22)) || ((command >= 53) && (command <= 63))) {
            botIncCommandAddress(this, command);
        }
//.......  выход из функции и передача управления следующему боту   ........
//.......  но перед выходом нужно проверить, входит ли бот в        ........
//.......  многоклеточную цепочку и если да, то нужно распределить  ........
//.......  энергию и минералы с соседями                            ........
//.......  также проверить, количество накопленой энергии, возможно ........
//.......  пришло время подохнуть или породить потомка              ........

        if (alive == LV_ALIVE) {
            int a = isMulti(this);
            // распределяем энергию  минералы по многоклеточному организму
            // возможны три варианта, бот находится внутри цепочки
            // бот имеет предыдущего бота в цепочке и не имеет следующего
            // бот имеет следующего бота в цепочке и не имеет предыдущего
            if (a == 3) {                 // бот находится внутри цепочки
                Bot pb = mprev; // ссылка на предыдущего бота в цепочке
                Bot nb = mnext; // ссылка на следующего бота в цепочке
                // делим минералы .................................................................
                int m = mineral + nb.mineral + pb.mineral; // общая сумма минералов
                //распределяем минералы между всеми тремя ботами
                m = m / 3;
                mineral = m;
                nb.mineral = m;
                pb.mineral = m;
                    // делим энергию ................................................................
                    // проверим, являются ли следующий и предыдущий боты в цепочке крайними .........
                    // если они не являются крайними, то распределяем энергию поровну       .........
                    // связанно это с тем, что в крайних ботах в цепочке должно быть больше энергии ..
                    // что бы они плодили новых ботов и удлиняли цепочку
                int apb = isMulti(pb);
                int anb = isMulti(nb);
                if ((anb == 3) && (apb == 3)) { // если следующий и предыдущий боты не являются крайними
                                                 // то распределяем энергию поровну
                    int h =  health + nb.health + pb.health;
                    h = h / 3;
                    health = h;
                    nb.health = h;
                    pb.health = h;
                }
            }
            // бот является крайним в цепочке и имеет предыдкщего бота
            if (a == 1) {
                Bot pb = mprev; // ссылка на предыдущего бота
                int apb = isMulti(pb);  // проверим, является ли предыдущий бот крайним в цепочке
                if (apb == 3) {   // если нет, то распределяем энергию в пользу текущего бота
                                   // так как он крайний и ему нужна энергия для роста цепочки
                    int h =  health + pb.health;
                    h = h / 4;
                    health = h * 3;
                    pb.health = h;
                }
            }
            // бот является крайним в цепочке и имеет следующего бота
            if (a == 2) {
                Bot nb = mnext; // ссылка на следующего бота
                int anb = isMulti(nb);   // проверим, является ли следующий бот крайним в цепочке
                if (anb == 3) {      // если нет, то распределяем энергию в пользу текущего бота
                                      // так как он крайний и ему нужна энергия для роста цепочки
                    int h =  health + nb.health;
                    h = h / 4;
                    health = h * 3;
                    nb.health = h;
                }
            }
            //... проверим уровень энергии у бота, возможно пришла пора помереть или родить
            if (health > 999) {    // если энергии больше 999, то плодим нового бота
                if ((a == 1) || (a == 2)) {
                    botMulti(this); // если бот был крайним в цепочке, то его потомок входит в состав цепочки
                } else {
                    botDouble(this); // если бот был свободным или находился внутри цепочки
                }
            }                                               // то его потомок рождается свободным
            health =  health - 3;   // каждый ход отнимает 3 единички здоровья(энегрии)
            if (health < 1) {                    // если энергии стало меньше 1
                bot2Organic(this);                                // то время умирать, превращаясь в огранику
                return;                        // и передаем управление к следующему боту
            }
            // если бот находится на глубине ниже 48 уровня
            // то он автоматом накапливает минералы, но не более 999
            if (y > World.simulation.height / 2) {
                mineral = mineral + 1;
                if (y > World.simulation.height / 6 * 4) { mineral = mineral + 1; }
                if (y > World.simulation.height / 6 * 5) { mineral = mineral + 1; }
                if (mineral > 999) { mineral = 999; }
            }
        }
    }

    // -- получение Х-координаты рядом        ---------
    //  с био по относительному направлению  ----------
    // in - номер бота, направление       --------------
    // out - X -  координата             --------------
    public int xFromVektorR(Bot bot, int n) {
        int xt = bot.x;
        n = n + bot.direction;
        if (n >= 8) {
            n = n - 8;
        }
        if (n == 0 || n == 6 || n == 7) {
            xt = xt - 1;
            if (xt == -1) {
                xt = World.simulation.width - 1;
            }
        } else if (n == 2 || n == 3 || n == 4) {
            xt = xt + 1;
            if (xt == World.simulation.width) {
                xt = 0;
            }
        }
        return xt;
    }
    // -- получение Х-координаты рядом        ---------
    //  с био по абсолютному направлению     ----------
    // in - номер био, направление       --------------
    // out - X -  координата             --------------
    public int xFromVektorA(Bot bot, int n) {
        int xt = bot.x;
        if (n == 0 || n == 6 || n == 7) {
            xt = xt - 1;
            if (xt == -1) {
                xt = World.simulation.width - 1;
            }
        } else if (n == 2 || n == 3 || n == 4) {
            xt = xt + 1;
            if (xt == World.simulation.width) {
                xt = 0;
            }
        }
        return xt;
    }

    // ------ получение Y-координаты рядом              ---------
    // ---- Y координата по относительному направлению  ----------
    // ---  in - номер бота, направление              ------------
    // ---  out - Y -  координата                    -------------
    public int yFromVektorR(Bot bot, int n) {
        int yt = bot.y;
        n = n + bot.direction;
        if (n >= 8) {
            n = n - 8;
        }
        if (n == 0 || n == 1 || n == 2) {
            yt = yt - 1;
        } else if (n == 4 || n == 5 || n == 6) {
            yt = yt + 1;
        }
        return yt;
    }
    // ------ получение Y-координаты рядом              ---------
    // ---- Y координата по абсолютному направлению     ----------
    // ---  in - номер бота, направление              ------------
    // ---  out - Y -  координата                    -------------
    public int yFromVektorA(Bot bot, int n) {
        int yt = bot.y;
        if (n == 0 || n == 1 || n == 2) {
            yt = yt - 1;
        } else if (n == 4 || n == 5 || n == 6) {
            yt = yt + 1;
        }
        return yt;
    }

    //===========   окружен ли бот          ==========
    // ---  in - бот                 ------------
    //===== out  1-окружен  2-нет           ===
    public int fullAroud(Bot bot) {
        for (int i = 0; i < 8; i++) {
            int xt = xFromVektorR(bot, i);
            int yt = yFromVektorR(bot, i);
            if ((yt >= 0) && (yt < World.simulation.height)) {
                if (World.simulation.matrix[xt][yt] == null) {
                    return 2;
                }
            }
        }
        return 1;
    }

    //==== ищет свободные ячейки вокруг бота ============
    //==== начинает спереди и дальше по      ============
    //==== кругу через низ    ( world )      ============
    //==== in  - бот                  ============
    //==== out - номер направление или       ============
    //====  или 8 , если свободных нет       ============
    public int findEmptyDirection(Bot bot) {
        for (int i = 0; i < 8; i++) {
            int xt = xFromVektorR(bot, i);
            int yt = yFromVektorR(bot, i);
            if ((yt >= 0) && (yt < World.simulation.height)) {
                if (World.simulation.matrix[xt][yt] == null) {
                    return i;
                }
            }
        }
        //........no empty..........
        return 8;
    }

    // -- получение параметра для команды   --------------
    //  in - bot
    // out - возвращает число из днк, следующее за выполняемой командой
    public int botGetParam(Bot bot) {
        int paramadr = bot.adr + 1;
        if (paramadr >= MIND_SIZE) {
            paramadr = paramadr - MIND_SIZE;
        }
        return bot.mind[paramadr]; // возвращает число, следующее за выполняемой командой
    }

    // -- увеличение адреса команды   --------------
    //  in - bot, насколько прибавить адрес --
    public void botIncCommandAddress(Bot bot, int a) {
        int paramadr = bot.adr + a;
        if (paramadr >= MIND_SIZE) {
            paramadr = paramadr - MIND_SIZE;
        }
        bot.adr = paramadr;
    }

    //---- косвенное увеличение адреса команды   --------------
    //---- in - номер bot, смещение до команды,  --------------
    //---- которая станет смещением              --------------
    public void botIndirectIncCmdAddress(Bot bot, int a) {
        int paramadr = bot.adr + a;
        if (paramadr >= MIND_SIZE) {
            paramadr = paramadr - MIND_SIZE;
        }
        int bias = bot.mind[paramadr];
        botIncCommandAddress(bot, bias);
    }

    //=====  превращение бота в органику    ===========
    //=====  in - номер бота                ===========
    public void bot2Organic(Bot bot) {
        bot.alive = LV_ORGANIC_HOLD;       // отметим в массиве bots[], что бот органика
        Bot pbot = bot.mprev;
        Bot nbot = bot.mnext;
        if (pbot != null){ pbot.mnext = null; } // удаление бота из многоклеточной цепочки
        if (nbot != null){ nbot.mprev = null; }
        bot.mprev = null;
        bot.mnext = null;
    }

    //========   нахожусь ли я в многоклеточной цепочке  =====
    //========   in - номер бота                         =====
    //========   out- 0 - нет, 1 - есть MPREV, 2 - есть MNEXT, 3 есть MPREV и MNEXT
    public int isMulti(Bot bot) {
        int a = 0;
        if (bot.mprev != null) {
            a = 1;
        }
        if (bot.mnext != null) {
            a = a + 2;
        }
        return a;
    }

    //===== перемещает бота в нужную точку  ==============
    //===== без проверок                    ==============
    //===== in - номер бота и новые координаты ===========
    public void moveBot(Bot bot, int xt, int yt) {
        World.simulation.matrix[xt][yt] = bot;
        World.simulation.matrix[bot.x][bot.y] = null;
        bot.x = xt;
        bot.y = yt;
    }

    //=====   удаление бота        =============
    //=====  in - бот       =============
    public void deleteBot(Bot bot) {
        Bot pbot = bot.mprev;
        Bot nbot = bot.mnext;
        if (pbot != null){ pbot.mnext = null; } // удаление бота из многоклеточной цепочки
        if (nbot != null){ nbot.mprev = null; }
        bot.mprev = null;
        bot.mnext = null;
        World.simulation.matrix[bot.x][bot.y] = null; // удаление бота с карты
    }


    //=========================================================================================
    //============================       КОД КОМАНД   =========================================
    //=========================================================================================
    // ...  фотосинтез, этой командой забит геном первого бота     ...............
    // ...  бот получает энергию солнца в зависимости от глубины   ...............
    // ...  и количества минералов, накопленных ботом              ...............
    public void botEatSun(Bot bot) {
        int t;
        if (bot.mineral < 100) {
            t = 0;
        } else if (bot.mineral < 400) {
            t = 1;
        } else {
            t = 2;
        }
        int a = 0;
        if (bot.mprev != null) {
            a = a + 4;
        }
        if (bot.mnext != null) {
            a = a + 4;
        }
        int hlt = a + 1 * (11 - (15 * bot.y / World.simulation.height) + t); // формула вычисления энергии ============================= SEZON!!!!!!!!!!
//        System.out.println(World.simulation.generation + ": " + bot.health + " + " + hlt);
        if (hlt > 0) {
            bot.health = bot.health + hlt;   // прибавляем полученную энергия к энергии бота
            goGreen(bot, hlt);                                     // бот от этого зеленеет
        }
    }


    // ...  преобразование минералов в энергию  ...............
    public void botMineral2Energy(Bot bot) {
        if (bot.mineral > 100) {   // максимальное количество минералов, которые можно преобразовать в энергию = 100
            bot.mineral = bot.mineral - 100;
            bot.health = bot.health + 400; // 1 минерал = 4 энергии
            goBlue(bot, 100);  // бот от этого синеет
        } else {  // если минералов меньше 100, то все минералы переходят в энергию
            goBlue(bot, bot.mineral);
            bot.health = bot.health + 4 * bot.mineral;
            bot.mineral = 0;
        }
    }

    //===========================  перемещение бота   ========================================
    public int botMove(Bot bot, int direction, int ra) { // ссылка на бота, направлелие и флажок(относительное или абсолютное направление)
        // на выходе   2-пусто  3-стена  4-органика 5-бот 6-родня
        int xt;
        int yt;
        if (ra == 0) {          // вычисляем координату клетки, куда перемещается бот (относительное направление)
            xt = xFromVektorR(bot, direction);
            yt = yFromVektorR(bot, direction);
        } else {                // вычисляем координату клетки, куда перемещается бот (абсолютное направление)
            xt = xFromVektorA(bot, direction);
            yt = yFromVektorA(bot, direction);
        }
        if ((yt < 0) || (yt >= World.simulation.height)) {  // если там ... стена
            return 3;                       // то возвращаем 3
        }
        if (World.simulation.matrix[xt][yt] == null) {  // если клетка была пустая,
            moveBot(bot, xt, yt);    // то перемещаем бота
            return 2;                       // и функция возвращает 2
        }
        // осталось 2 варианта: ограника или бот
        if (World.simulation.matrix[xt][yt].alive <= LV_ORGANIC_SINK) { // если на клетке находится органика
            return 4;                       // то возвращаем 4
        }
        if (isRelative(bot, World.simulation.matrix[xt][yt]) == 1) {  // если на клетке родня
            return 6;                      // то возвращаем 6
        }
        return 5;                         // остался только один вариант - на клетке какой-то бот возвращаем 5
    }

    //============================    скушать другого бота или органику  ==========================================
    public int botEat(Bot bot, int direction, int ra) { // на входе ссылка на бота, направлелие и флажок(относительное или абсолютное направление)
        // на выходе пусто - 2  стена - 3  органик - 4  бот - 5
        bot.health = bot.health - 4; // бот теряет на этом 4 энергии в независимости от результата
        int xt;
        int yt;
        if (ra == 0) {  // вычисляем координату клетки, с которой хочет скушать бот (относительное направление)
            xt = xFromVektorR(bot, direction);
            yt = yFromVektorR(bot, direction);
        } else {        // вычисляем координату клетки, с которой хочет скушать бот (абсолютное направление)
            xt = xFromVektorA(bot, direction);
            yt = yFromVektorA(bot, direction);
        }
        if ((yt < 0) || (yt >= World.simulation.height)) {  // если там стена возвращаем 3
            return 3;
        }
        if (World.simulation.matrix[xt][yt] == null) {  // если клетка пустая возвращаем 2
            return 2;
        }
        // осталось 2 варианта: ограника или бот
        else if (World.simulation.matrix[xt][yt].alive <= LV_ORGANIC_SINK) {   // если там оказалась органика
            deleteBot(World.simulation.matrix[xt][yt]);                           // то удаляем её из списков
            bot.health = bot.health + 100; //здоровье увеличилось на 100
            goRed(this, 100);                                     // бот покраснел
            return 4;                                               // возвращаем 4
        }
        //--------- дошли до сюда, значит впереди живой бот -------------------
        int min0 = bot.mineral;  // определим количество минералов у бота
        int min1 = World.simulation.matrix[xt][yt].mineral;  // определим количество минералов у потенциального обеда
        int hl = World.simulation.matrix[xt][yt].health;  // определим энергию у потенциального обеда
        // если у бота минералов больше
        if (min0 >= min1) {
            bot.mineral = min0 - min1; // количество минералов у бота уменьшается на количество минералов у жертвы
            // типа, стесал свои зубы о панцирь жертвы
            deleteBot(World.simulation.matrix[xt][yt]);          // удаляем жертву из списков
            int cl = 100 + (hl / 2);           // количество энергии у бота прибавляется на 100+(половина от энергии жертвы)
            bot.health = bot.health + cl;
            goRed(this, cl);                    // бот краснеет
            return 5;                              // возвращаем 5
        }
        //если у жертвы минералов больше ----------------------
        bot.mineral = 0; // то бот израсходовал все свои минералы на преодоление защиты
        min1 = min1 - min0;       // у жертвы количество минералов тоже уменьшилось
        World.simulation.matrix[xt][yt].mineral = min1 - min0;       // перезаписали минералы жертве =========================ЗАПЛАТКА!!!!!!!!!!!!
        //------ если здоровья в 2 раза больше, чем минералов у жертвы  ------
        //------ то здоровьем проламываем минералы ---------------------------
        if (bot.health >= 2 * min1) {
            deleteBot(World.simulation.matrix[xt][yt]);         // удаляем жертву из списков
            int cl = 100 + (hl / 2) - 2 * min1; // вычисляем, сколько энергии смог получить бот
            bot.health = bot.health + cl;
            if (cl < 0) { cl = 0; } //========================================================================================ЗАПЛАТКА!!!!!!!!!!! - энергия не должна быть отрицательной

            goRed(this, cl);                   // бот краснеет
            return 5;                             // возвращаем 5
        }
        //--- если здоровья меньше, чем (минералов у жертвы)*2, то бот погибает от жертвы
        World.simulation.matrix[xt][yt].mineral = min1 - (bot.health / 2);  // у жертвы минералы истраченны
        bot.health = 0;  // здоровье уходит в ноль
        return 5;                       // возвращаем 5
    }

    //.======================  посмотреть ==================================================
    public int botSeeBots(Bot bot, int direction, int ra) { // на входе ссылка на бота, направлелие и флажок(относительное или абсолютное направление)
        // на выходе  пусто - 2  стена - 3  органик - 4  бот - 5  родня - 6
        int xt;
        int yt;
        if (ra == 0) {  // выясняем, есть ли что в этом  направлении (относительном)
            xt = xFromVektorR(bot, direction);
            yt = yFromVektorR(bot, direction);
        } else {       // выясняем, есть ли что в этом  направлении (абсолютном)
            xt = xFromVektorA(bot, direction);
            yt = yFromVektorA(bot, direction);
        }
        if (yt < 0 || yt >= World.simulation.height) {  // если там стена возвращаем 3
            return 3;
        } else if (World.simulation.matrix[xt][yt] == null) {  // если клетка пустая возвращаем 2
            return 2;
        } else if (World.simulation.matrix[xt][yt].alive <= LV_ORGANIC_SINK) { // если органика возвращаем 4
            return 4;
        } else if (isRelative(bot, World.simulation.matrix[xt][yt]) == 1) {  // если родня, то возвращаем 6
            return 6;
        } else { // если какой-то бот, то возвращаем 5
            return 5;
        }
    }


    //======== атака на геном соседа, меняем случайны ген случайным образом  ===============
    public void botGenAttack(Bot bot) {   // вычисляем кто у нас перед ботом (используется только относительное направление вперед)
        int xt = xFromVektorR(bot, 0);
        int yt = yFromVektorR(bot, 0);
        if ((yt >= 0) && (yt < World.simulation.height) && (World.simulation.matrix[xt][yt] != null)) {
            if (World.simulation.matrix[xt][yt].alive == LV_ALIVE) { // если там живой бот
                bot.health = bot.health - 10; // то атакуюий бот теряет на атаку 10 энергии
                if (bot.health > 0) {                    // если он при этом не умер
                    int ma = (int) (Math.random() * 64);  // 0..63 // то у жертвы случайным образом меняется один ген
                    int mc = (int) (Math.random() * 64);  // 0..63
                    World.simulation.matrix[xt][yt].mind[ma] = mc;
                }
            }
        }
    }


    //==========               поделится          ====================================================
    // =========  если у бота больше энергии или минералов, чем у соседа в заданном направлении  =====
    //==========  то бот делится излишками                                                       =====
    public int botCare(Bot bot, int direction, int ra) { // на входе ссылка на бота, направлелие и флажок(относительное или абсолютное направление)
        // на выходе стена - 2 пусто - 3 органика - 4 удачно - 5
        int xt;
        int yt;
        if (ra == 0) {  // определяем координаты для относительного направления
            xt = xFromVektorR(bot, direction);
            yt = yFromVektorR(bot, direction);
        } else {        // определяем координаты для абсолютного направления
            xt = xFromVektorA(bot, direction);
            yt = yFromVektorA(bot, direction);
        }
        if (yt < 0 || yt >= World.simulation.height) {  // если там стена возвращаем 3
            return 3;
        } else if (World.simulation.matrix[xt][yt] == null) {  // если клетка пустая возвращаем 2
            return 2;
        } else if (World.simulation.matrix[xt][yt].alive <= LV_ORGANIC_SINK) { // если органика возвращаем 4
            return 4;
        }
        //------- если мы здесь, то в данном направлении живой ----------
        int hlt0 = bot.health;         // определим количество энергии и минералов
        int hlt1 = World.simulation.matrix[xt][yt].health;  // у бота и его соседа
        int min0 = bot.mineral;
        int min1 = World.simulation.matrix[xt][yt].mineral;
        if (hlt0 > hlt1) {              // если у бота больше энергии, чем у соседа
            int hlt = (hlt0 - hlt1) / 2;   // то распределяем энергию поровну
            bot.health = bot.health - hlt;
            World.simulation.matrix[xt][yt].health = World.simulation.matrix[xt][yt].health + hlt;
        }
        if (min0 > min1) {              // если у бота больше минералов, чем у соседа
            int min = (min0 - min1) / 2;   // то распределяем их поровну
            bot.mineral = bot.mineral - min;
            World.simulation.matrix[xt][yt].mineral = World.simulation.matrix[xt][yt].mineral + min;
        }
        return 5;
    }


    //=================  отдать безвозместно, то есть даром    ==========
    public int botGive(Bot bot, int direction, int ra) // на входе ссылка на бота, направлелие и флажок(относительное или абсолютное направление)
    {                         // на выходе стена - 2 пусто - 3 органика - 4 удачно - 5
        int xt;
        int yt;
        if (ra == 0) {  // определяем координаты для относительного направления
            xt = xFromVektorR(bot, direction);
            yt = yFromVektorR(bot, direction);
        } else {        // определяем координаты для абсолютного направления
            xt = xFromVektorA(bot, direction);
            yt = yFromVektorA(bot, direction);
        }
        if (yt < 0 || yt >= World.simulation.height) {  // если там стена возвращаем 3
            return 3;
        } else if (World.simulation.matrix[xt][yt] == null) {  // если клетка пустая возвращаем 2
            return 2;
        } else if (World.simulation.matrix[xt][yt].alive <= LV_ORGANIC_SINK) { // если органика возвращаем 4
            return 4;
        }
        //------- если мы здесь, то в данном направлении живой ----------
        int hlt0 = bot.health;  // бот отдает четверть своей энергии
        int hlt = hlt0 / 4;
        bot.health = hlt0 - hlt;
        World.simulation.matrix[xt][yt].health = World.simulation.matrix[xt][yt].health + hlt;

        int min0 = bot.mineral;  // бот отдает четверть своих минералов
        if (min0 > 3) {                 // только если их у него не меньше 4
            int min = min0 / 4;
            bot.mineral = min0 - min;
            World.simulation.matrix[xt][yt].mineral = World.simulation.matrix[xt][yt].mineral + min;
            if (World.simulation.matrix[xt][yt].mineral > 999) {
                World.simulation.matrix[xt][yt].mineral = 999;
            }
        }
        return 5;
    }


    //....................................................................
    // рождение нового бота делением
    public void botDouble(Bot bot) {
        bot.health = bot.health - 150;      // бот затрачивает 150 единиц энергии на создание копии
        if (bot.health <= 0) {
            return;
        }   // если у него было меньше 150, то пора помирать

        int n = findEmptyDirection(bot);    // проверим, окружен ли бот
        if (n == 8) {                      // если бот окружен, то он в муках погибает
            bot.health = 0;
            return;
        }

        Bot newbot = new Bot();

        int xt = xFromVektorR(bot, n);   // координаты X и Y
        int yt = yFromVektorR(bot, n);

        for (int i = 0; i < MIND_SIZE; i++) {  // копируем геном в нового бота
            newbot.mind[i] = bot.mind[i];
        }
        if (Math.random() < 0.25) {     // в одном случае из четырех случайным образом меняем один случайный байт в геноме
            int ma = (int) (Math.random() * 64);  // 0..63
            int mc = (int) (Math.random() * 64);  // 0..63
            newbot.mind[ma] = mc;
        }

        newbot.adr = 0;                         // указатель текущей команды в новорожденном устанавливается в 0
        newbot.x = xt;
        newbot.y = yt;

        newbot.health = bot.health / 2;   // забирается половина здоровья у предка
        bot.health = bot.health / 2;
        newbot.mineral = bot.mineral / 2; // забирается половина минералов у предка
        bot.mineral = bot.mineral / 2;

        newbot.alive = 3;             // отмечаем, что бот живой

        newbot.c_red = bot.c_red;   // цвет такой же, как у предка
        newbot.c_green = bot.c_green;   // цвет такой же, как у предка
        newbot.c_blue = bot.c_blue;   // цвет такой же, как у предка

        newbot.direction = (int) (Math.random() * 8);   // направление, куда повернут новорожденный, генерируется случайно

        World.simulation.matrix[xt][yt] = newbot;    // отмечаем нового бота в массиве matrix
    }

    // ======       рождение новой клетки многоклеточного    ==========================================
    private void botMulti(Bot bot) {
        Bot pbot = bot.mprev;    // ссылки на предыдущего и следущего в многоклеточной цепочке
        Bot nbot = bot.mnext;
        // если обе ссылки больше 0, то бот уже внутри цепочки
        if ((pbot != null) && (nbot != null)) {
            return;
        } // поэтому выходим без создания нового бота

        bot.health = bot.health - 150; // бот затрачивает 150 единиц энергии на создание копии
        if (bot.health <= 0) {
            return;
        }// если у него было меньше 150, то пора помирать
        int n = findEmptyDirection(bot); // проверим, окружен ли бот

        if (n == 8) {  // если бот окружен, то он в муках погибает
            bot.health = 0;
            return;
        }
        Bot newbot = new Bot();

        int xt = xFromVektorR(bot, n);   // координаты X и Y
        int yt = yFromVektorR(bot, n);

        for (int i = 0; i < MIND_SIZE; i++) {  // копируем геном в нового бота
            newbot.mind[i] = newbot.mind[i];
        }
        if (Math.random() < 0.25) {     // в одном случае из четырех случайным образом меняем один случайный байт в геноме
            int ma = (int) (Math.random() * 64);  // 0..63
            int mc = (int) (Math.random() * 64);  // 0..63
            newbot.mind[ma] = mc;
        }

        newbot.adr = 0;                         // указатель текущей команды в новорожденном устанавливается в 0
        newbot.x = xt;
        newbot.y = yt;

        newbot.health = bot.health / 2;   // забирается половина здоровья у предка
        bot.health = bot.health / 2;
        newbot.mineral = bot.mineral / 2; // забирается половина минералов у предка
        bot.mineral = bot.mineral / 2;

        newbot.alive = 3;             // отмечаем, что бот живой

        newbot.c_red = bot.c_red;   // цвет такой же, как у предка
        newbot.c_green = bot.c_green;   // цвет такой же, как у предка
        newbot.c_blue = bot.c_blue;   // цвет такой же, как у предка

        newbot.direction = (int) (Math.random() * 8);   // направление, куда повернут новорожденный, генерируется случайно

        World.simulation.matrix[xt][yt] = newbot;    // отмечаем нового бота в массиве matrix

        if (nbot == null) {                      // если у бота-предка ссылка на следующего бота в многоклеточной цепочке пуста
            bot.mnext = newbot; // то вставляем туда новорожденного бота
            newbot.mprev = bot;    // у новорожденного ссылка на предыдущего указывает на бота-предка
            newbot.mnext = null;       // ссылка на следующего пуста, новорожденный бот является крайним в цепочке
        } else {                              // если у бота-предка ссылка на предыдущего бота в многоклеточной цепочке пуста
            bot.mprev = newbot; // то вставляем туда новорожденного бота
            newbot.mnext = bot;    // у новорожденного ссылка на следующего указывает на бота-предка
            newbot.mprev = null;       // ссылка на предыдущего пуста, новорожденный бот является крайним в цепочке
        }
    }

    //========   копится ли энергия            =====
    //========   in - номер бота                =====
    //========   out- 1 - да, 2 - нет           =====
    public int isHealthGrow(Bot bot) {
        int t;
        if (bot.mineral < 100) {
            t = 0;
        } else {
            if (bot.mineral < 400) {
                t = 1;
            } else {
                t = 2;
            }
        }
        int hlt = 10 - (15 * bot.y / World.simulation.height) + t; // ====================================================== SEZON!!!!!!!!!!!!!!!!!!
        if (hlt >= 3) {
            return 1;
        } else {
            return 2;
        }
    }

    //========   родственники ли боты?              =====
    //========   in - номер 1 бота , номер 2 бота   =====
    //========   out- 0 - нет, 1 - да               =====
    public int isRelative(Bot bot0, Bot bot1) {
        if (bot1.alive != LV_ALIVE) {
            return 0;
        }
        int dif = 0;    // счетчик несовпадений в геноме
        for (int i = 0; i < MIND_SIZE; i++) {
            if (bot0.mind[i] != bot1.mind[i]) {
                dif = dif + 1;
                if (dif == 2) {
                    return 0;
                } // если несовпадений в генеме больше 1
            }                               // то боты не родственики
        }
        return 1;
    }

    //=== делаем бота более зеленым на экране         ======
    //=== in - номер бота, на сколько озеленить       ======
    public void goGreen(Bot bot, int num) {  // добавляем зелени
        bot.c_green = bot.c_green + num;
        if (bot.c_green + num > 255) {
            bot.c_green = 255;
        }
        int nm = num / 2;
        // убавляем красноту
        bot.c_red = bot.c_red - nm;
        if (bot.c_red < 0) {
            bot.c_blue = bot.c_blue +  bot.c_red;
        }
        // убавляем синеву
        bot.c_blue = bot.c_blue - nm;
        if (bot.c_blue < 0 ) {
            bot.c_red = bot.c_red + bot.c_blue;
        }
        if (bot.c_red < 0) {
            bot.c_red = 0;
        }
        if (bot.c_blue < 0) {
            bot.c_blue = 0;
        }
    }

    //=== делаем бота более синим на экране         ======
    //=== in - номер бота, на сколько осинить       ======
    public void goBlue(Bot bot, int num) {  // добавляем синевы
        bot.c_blue = bot.c_blue + num;
        if (bot.c_blue > 255) {
            bot.c_blue = 255;
        }
        int nm = num / 2;
        // убавляем зелень
        bot.c_green = bot.c_green - nm;
        if (bot.c_green < 0 ) {
            bot.c_red = bot.c_red + bot.c_green;
        }
        // убавляем красноту
        bot.c_red = bot.c_red - nm;
        if (bot.c_red < 0) {
            bot.c_green = bot.c_green +  bot.c_red;
        }
        if (bot.c_red < 0) {
            bot.c_red = 0;
        }
        if (bot.c_green < 0) {
            bot.c_green = 0;
        }
    }

    //=== делаем бота более красным на экране         ======
    //=== in - номер бота, на сколько окраснить       ======
    public void goRed(Bot bot, int num) {  // добавляем красноты
        bot.c_red = bot.c_red + num;
        if (bot.c_red > 255) {
            bot.c_red = 255;
        }
        int nm = num / 2;
        // убавляем зелень
        bot.c_green = bot.c_green - nm;
        if (bot.c_green < 0 ) {
            bot.c_blue = bot.c_blue + bot.c_green;
        }
        // убавляем синеву
        bot.c_blue = bot.c_blue - nm;
        if (bot.c_blue < 0) {
            bot.c_green = bot.c_green +  bot.c_blue;
        }
        if (bot.c_blue < 0) {
            bot.c_blue = 0;
        }
        if (bot.c_green < 0) {
            bot.c_green = 0;
        }
    }




}