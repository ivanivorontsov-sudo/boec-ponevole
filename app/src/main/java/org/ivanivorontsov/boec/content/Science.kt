package org.ivanivorontsov.boec.content

object Science {
    val principles: List<InfoCard> = listOf(
        InfoCard("Сколько тренироваться", listOf(
            "Взрослым: 150–300 минут умеренной аэробной нагрузки в неделю (или 75–150 интенсивной) и силовые упражнения на все крупные мышцы минимум 2 дня в неделю.",
            "Любая силовая тренировка лучше, чем ничего: главный скачок пользы — переход от нуля к регулярным занятиям.",
        ), listOf("ВОЗ: рекомендации по физической активности, 2020", "ACSM Position Stand, 2026")),
        InfoCard("Частота: 2+ раза в неделю на мышцу", listOf(
            "Тренировка каждой мышцы минимум дважды в неделю даёт больший рост, чем один раз, при равном объёме.",
            "Поэтому план строится по схемам «всё тело» или «верх/низ», а не «одна мышца в день».",
        ), listOf("Schoenfeld, Ogborn, Krieger. Sports Med, 2016", "ACSM Position Stand, 2026")),
        InfoCard("Объём: подходы в неделю", listOf(
            "Минимум 2 подхода на упражнение. Для роста мышц — около 10 и более тяжёлых подходов на мышцу в неделю; польза растёт примерно до 18–20, дальше — всё меньше.",
            "Новичкам хватает меньшего объёма: начни с нижней границы и добавляй постепенно.",
        ), listOf("Schoenfeld, Ogborn, Krieger. J Sports Sci, 2017", "ACSM Position Stand, 2026")),
        InfoCard("Интенсивность: RPE и «повторы в запасе»", listOf(
            "RPE — субъективная тяжесть по шкале 1–10. RPE 8 = могу ещё 2 повтора, RPE 10 = больше ни одного.",
            "Для роста мышц подходит широкий диапазон повторов (≈6–30), если подход близок к отказу. Для силы нужны более тяжёлые варианты и мало повторов.",
            "Новичкам — RPE 6–7, затем 7–9. До отказа каждый подход идти не нужно.",
        ), listOf("Zourdos et al. J Strength Cond Res, 2016 (шкала RIR)", "Schoenfeld et al. J Strength Cond Res, 2017 (лёгкие vs тяжёлые веса)")),
        InfoCard("Отдых между подходами", listOf(
            "Для силы и в базовых упражнениях отдых 2–3 минуты даёт лучший результат, чем 30–60 секунд.",
            "В изолирующих упражнениях и упражнениях на кор хватает 60–90 секунд.",
        ), listOf("Grgic et al. Sports Med, 2018", "Schoenfeld et al. J Strength Cond Res, 2016")),
        InfoCard("Прогрессия нагрузки", listOf(
            "Двойная прогрессия: держи диапазон повторов (например, 8–12). Когда во всех подходах сделал верхнюю границу с RPE ≤ 8 — бери более сложный вариант упражнения или +2–5 % веса.",
            "Без инвентаря усложняй: медленный темп, паузы, неустойчивость, одна нога или рука.",
        ), listOf("ACSM Position Stand, 2009 и 2026")),
        InfoCard("Разминка", listOf(
            "5–10 минут: лёгкая аэробная часть (ходьба, прыжки «звёздочка») + динамическая подвижность + 1–2 лёгких подхода первого упражнения.",
            "Разминка улучшает результат в большинстве исследований; статическую растяжку лучше оставить на заминку.",
        ), listOf("Fradkin et al. J Strength Cond Res, 2010")),
        InfoCard("Разгрузочная неделя", listOf(
            "Каждая 5-я неделя — разгрузочная: подходов примерно вдвое меньше, RPE на 1–2 ниже. Это помогает восстановиться и снизить усталость суставов.",
            "Если чувствуешь накопленную усталость, плохой сон, падение результатов — сделай разгрузку раньше.",
        ), listOf("Bell et al. Sports Medicine – Open, 2022–2023 (практика разгрузок)")),
        InfoCard("Сон и восстановление", listOf(
            "Взрослым нужно не меньше 7 часов сна. Недосып снижает силу, выносливость и повышает риск травм.",
        ), listOf("Watson et al. (AASM/SRS), Sleep, 2015")),
        InfoCard("Боль — сигнал остановиться", listOf(
            "Лёгкая крепатура через 1–2 дня — норма. Острая, колющая, нарастающая боль в суставе или мышце — прекрати упражнение.",
            "Немедленно прекращай тренировку и звони 112 при боли или давлении в груди, сильной одышке, головокружении, обмороке, нарушении речи.",
        ), listOf("PAR-Q+ Collaboration", "ACSM's Guidelines for Exercise Testing and Prescription")),
    )

    val disclaimer = listOf(
        "Приложение — образовательный инструмент, а не медицинская услуга. Оно не ставит диагнозы и не заменяет врача, тренера или инструктора по самообороне.",
        "Перед началом занятий, при хронических болезнях, беременности, после травм и операций проконсультируйся с врачом.",
        "Прекрати упражнение при острой боли, головокружении, боли в груди или сильной одышке. При угрозе жизни звони 112.",
        "Советы по первой помощи основаны на открытых рекомендациях ВОЗ, Минздрава, МЧС и Роспотребнадзора, но не заменяют обучение первой помощи и вызов скорой.",
        "Сведения о необходимой обороне — общая справка, а не юридическая консультация.",
    )

    /** Seven general-health questions of PAR-Q+ (adapted Russian wording). */
    val parq = listOf(
        "Говорил ли тебе врач, что у тебя есть заболевание сердца или повышенное артериальное давление?",
        "Чувствуешь ли ты боль в груди в покое, в повседневной жизни или при физической нагрузке?",
        "Терял(а) ли ты равновесие из-за головокружения или сознание за последние 12 месяцев? (Ответь «нет», если головокружение было только от учащённого дыхания при интенсивной нагрузке.)",
        "Есть ли у тебя другое хроническое заболевание (кроме болезней сердца и давления)?",
        "Принимаешь ли ты сейчас назначенные врачом лекарства от хронического заболевания?",
        "Есть ли сейчас (или были в последние 12 месяцев) проблемы с костями, суставами, мышцами, связками или сухожилиями, которые может усилить нагрузка?",
        "Говорил ли тебе врач, что заниматься можно только под медицинским наблюдением?",
    )

    val parqYes = listOf(
        "Ты ответил(а) «да» хотя бы на один вопрос. По правилам PAR-Q+ перед повышением нагрузки нужно заполнить расширенную часть анкеты или проконсультироваться с врачом.",
        "Пока включён осторожный режим: только уровень «новичок», RPE не выше 6–7, без спринтов и прыжков. Изменить можно в настройках после разрешения врача.",
    )

    data class Source(val title: String, val detail: String)

    val sources = listOf(
        Source("ВОЗ, 2020", "WHO guidelines on physical activity and sedentary behaviour. Geneva: WHO, 2020."),
        Source("ACSM, 2026", "American College of Sports Medicine Position Stand. Resistance Training Prescription for Muscle Function, Hypertrophy, and Physical Performance in Healthy Adults: An Overview of Reviews. Med Sci Sports Exerc. 2026;58(4)."),
        Source("ACSM, 2009", "Progression models in resistance training for healthy adults. Med Sci Sports Exerc. 2009;41(3):687-708."),
        Source("Schoenfeld et al., 2016", "Effects of resistance training frequency on measures of muscle hypertrophy: a systematic review and meta-analysis. Sports Med. 2016;46(11):1689-1697."),
        Source("Schoenfeld et al., 2017", "Dose-response relationship between weekly resistance training volume and increases in muscle mass. J Sports Sci. 2017;35(11):1073-1082."),
        Source("Schoenfeld et al., 2017b", "Strength and hypertrophy adaptations between low- vs. high-load resistance training. J Strength Cond Res. 2017;31(12):3508-3523."),
        Source("Schoenfeld et al., 2016b", "Longer interset rest periods enhance muscle strength and hypertrophy in resistance-trained men. J Strength Cond Res. 2016;30(7):1805-1812."),
        Source("Grgic et al., 2018", "Effects of rest interval duration in resistance training on measures of muscular strength: a systematic review. Sports Med. 2018;48(1):137-151."),
        Source("Zourdos et al., 2016", "Novel resistance training-specific RPE scale measuring repetitions in reserve. J Strength Cond Res. 2016;30(1):267-275."),
        Source("Fradkin et al., 2010", "Effects of warming-up on physical performance: a systematic review with meta-analysis. J Strength Cond Res. 2010;24(1):140-148."),
        Source("Bell et al.", "Исследования практики разгрузочных недель у тренеров и спортсменов. Sports Medicine – Open, 2022–2023."),
        Source("PAR-Q+", "The Physical Activity Readiness Questionnaire for Everyone. PAR-Q+ Collaboration (eparmedx.com). В приложении — адаптированный перевод общих вопросов."),
        Source("AASM/SRS, 2015", "Watson NF et al. Recommended amount of sleep for a healthy adult. Sleep. 2015;38(6):843-844."),
        Source("EFSA, 2010", "Scientific Opinion on Dietary Reference Values for water. EFSA Journal 2010;8(3):1459."),
        Source("УК РФ", "Статьи 37 (необходимая оборона) и 39 (крайняя необходимость)."),
        Source("Пленум ВС РФ", "Постановление от 27.09.2012 № 19 «О применении судами законодательства о необходимой обороне…» (ред. от 31.05.2022)."),
        Source("ВОЗ: бешенство", "Rabies — fact sheet; Frequently asked questions about rabies for the general public."),
        Source("Минздрав России, 2025", "Рекомендации по первой помощи при укусе змеи."),
        Source("МЧС России", "Памятки: первая помощь при укусе змеи, поведение в лесу и на воде."),
        Source("НИИ СП им. Джанелидзе", "Укус гадюки обыкновенной: рекомендации врачей-токсикологов."),
        Source("Роспотребнадзор", "Профилактика клещевых инфекций (2026); профилактика бешенства и ГЛПС; СанПиН 3.3686-21."),
        Source("CDC", "Tick removal; Preventing dog bites; Hantavirus: clean up safely."),
        Source("U.S. NPS", "Staying Safe Around Bears; bear pepper spray."),
        Source("ADF&G", "Alaska Department of Fish and Game: Living with Moose."),
        Source("WAO, 2020", "World Allergy Organization Anaphylaxis Guidance 2020."),
        Source("Центр «Амурский тигр» / WWF России", "Памятки о поведении при встрече с тигром."),
        Source("AAO", "American Academy of Ophthalmology: правило 20-20-20 для глаз."),
        Source("dontkillmyapp.com", "Инструкции по отключению агрессивной оптимизации батареи у производителей (realme, OPPO, Xiaomi, Samsung и др.)."),
    )
}
