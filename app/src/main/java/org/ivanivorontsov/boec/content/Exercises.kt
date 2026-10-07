package org.ivanivorontsov.boec.content

import org.ivanivorontsov.boec.model.Equipment.BAND
import org.ivanivorontsov.boec.model.Equipment.BAR
import org.ivanivorontsov.boec.model.Equipment.DUMBBELLS
import org.ivanivorontsov.boec.model.Exercise
import org.ivanivorontsov.boec.model.Muscle.*
import org.ivanivorontsov.boec.model.Slot

/**
 * Exercise library. Bodyweight first; dumbbells / bar / band optional.
 * Technique cues follow common strength & conditioning practice (NSCA, ACSM).
 */
object Exercises {
    val all: List<Exercise> = listOf(
        // ---------- SQUAT ----------
        Exercise(
            "sq_box", "Присед на стул", Slot.SQUAT, listOf(QUADS, GLUTES), listOf(CORE), difficulty = 1,
            cues = listOf("Стопы на ширине плеч, носки чуть врозь", "Таз назад и вниз, лёгкое касание стула — и вверх", "Колени идут по линии носков"),
            mistakes = listOf("Плюхаться на стул", "Колени заваливаются внутрь"),
            contra = listOf("Острая боль в колене или тазобедренном суставе"), harder = "sq_bw",
        ),
        Exercise(
            "sq_bw", "Приседания с собственным весом", Slot.SQUAT, listOf(QUADS, GLUTES), listOf(CORE, HAMSTRINGS), difficulty = 2,
            cues = listOf("Вес на всей стопе, пятки не отрываются", "Спина нейтральная, грудь открыта", "Глубина — пока держишь спину ровной"),
            mistakes = listOf("Округление поясницы внизу", "Отрыв пяток"),
            contra = listOf("Острая боль в колене"), easier = "sq_box", harder = "sq_tempo",
        ),
        Exercise(
            "sq_tempo", "Приседания с паузой и медленным опусканием", Slot.SQUAT, listOf(QUADS, GLUTES), listOf(CORE), difficulty = 3,
            cues = listOf("3 секунды вниз, 1 секунда пауза внизу, мощно вверх", "Дыши: вдох вниз, выдох на подъёме"),
            mistakes = listOf("Сокращать паузу", "Терять напряжение внизу"),
            contra = listOf("Острая боль в колене"), easier = "sq_bw", harder = "sq_pistol_box",
        ),
        Exercise(
            "sq_goblet", "Гоблет-присед с гантелью", Slot.SQUAT, listOf(QUADS, GLUTES), listOf(CORE, UPPER_BACK), equipment = DUMBBELLS, difficulty = 3,
            cues = listOf("Гантель вертикально у груди, локти вниз", "Опускайся между пятками, корпус вертикальный"),
            mistakes = listOf("Гантель уводит корпус вперёд", "Колени внутрь"),
            contra = listOf("Острая боль в колене или пояснице"), easier = "sq_bw",
        ),
        Exercise(
            "sq_pistol_box", "Присед на одной ноге на возвышение", Slot.SQUAT, listOf(QUADS, GLUTES), listOf(CORE), difficulty = 4, perSide = true,
            cues = listOf("Садись на устойчивый стул на одной ноге, вторая вперёд", "Руки вперёд для баланса", "Встать без рывка"),
            mistakes = listOf("Колено уходит внутрь", "Падение на стул"),
            contra = listOf("Нестабильность колена, проблемы с равновесием"), easier = "sq_tempo",
        ),
        // ---------- HINGE ----------
        Exercise(
            "hi_bridge", "Ягодичный мост", Slot.HINGE, listOf(GLUTES), listOf(HAMSTRINGS, CORE), difficulty = 1,
            cues = listOf("Лёжа на спине, стопы близко к тазу", "Подними таз, сжимая ягодицы, пауза 1–2 с", "Рёбра вниз — не прогибай поясницу"),
            mistakes = listOf("Выгибать поясницу вместо работы ягодиц", "Отрывать пятки"),
            contra = listOf("Острая боль в пояснице"), harder = "hi_sl_bridge",
        ),
        Exercise(
            "hi_sl_bridge", "Ягодичный мост на одной ноге", Slot.HINGE, listOf(GLUTES, HAMSTRINGS), listOf(CORE), difficulty = 2, perSide = true,
            cues = listOf("Одна нога согнута, вторая прямая вверх", "Таз не перекашивается"),
            mistakes = listOf("Перекос таза", "Рывки"),
            contra = listOf("Острая боль в пояснице"), easier = "hi_bridge", harder = "hi_sl_rdl",
        ),
        Exercise(
            "hi_sl_rdl", "Румынская тяга на одной ноге", Slot.HINGE, listOf(HAMSTRINGS, GLUTES), listOf(LOWER_BACK, CORE), difficulty = 3, perSide = true,
            cues = listOf("Наклон из таза, спина ровная, опорное колено чуть согнуто", "Свободная нога уходит назад в одну линию с корпусом", "Можно держаться за стену"),
            mistakes = listOf("Круглая спина", "Разворот таза наружу"),
            contra = listOf("Острая боль в пояснице, проблемы с равновесием"), easier = "hi_sl_bridge", harder = "hi_slider_curl",
        ),
        Exercise(
            "hi_db_rdl", "Румынская тяга с гантелями", Slot.HINGE, listOf(HAMSTRINGS, GLUTES), listOf(LOWER_BACK, FOREARMS), equipment = DUMBBELLS, difficulty = 3,
            cues = listOf("Гантели скользят вдоль бёдер", "Таз назад до натяжения задней поверхности бедра", "Спина нейтральная всё время"),
            mistakes = listOf("Приседать вместо наклона", "Округлять спину ради глубины"),
            contra = listOf("Грыжа или острая боль в пояснице — только после разрешения врача"), easier = "hi_bridge",
        ),
        Exercise(
            "hi_slider_curl", "Сгибание ног на полотенце", Slot.HINGE, listOf(HAMSTRINGS), listOf(GLUTES), difficulty = 3,
            cues = listOf("Лёжа на спине, пятки на полотенце на гладком полу", "Таз поднят, тяни пятки к себе и медленно отпускай"),
            mistakes = listOf("Опускать таз", "Бросать ноги на возврате"),
            contra = listOf("Судороги или травма задней поверхности бедра"), easier = "hi_sl_bridge", harder = "hi_nordic",
        ),
        Exercise(
            "hi_nordic", "Нордические сгибания (негативы)", Slot.HINGE, listOf(HAMSTRINGS), listOf(GLUTES), difficulty = 5,
            cues = listOf("Пятки надёжно зафиксированы (диван, партнёр)", "Медленно опускайся корпусом вперёд 3–5 с, ловись руками"),
            mistakes = listOf("Сгибаться в тазу", "Падать без контроля"),
            contra = listOf("Свежая травма задней поверхности бедра"), easier = "hi_slider_curl",
        ),
        // ---------- LUNGE ----------
        Exercise(
            "lu_split", "Статические выпады (сплит-присед)", Slot.LUNGE, listOf(QUADS, GLUTES), listOf(HAMSTRINGS, CORE), difficulty = 2, perSide = true,
            cues = listOf("Широкий шаг, обе стопы смотрят вперёд", "Опускай заднее колено вертикально вниз", "Корпус ровный"),
            mistakes = listOf("Слишком узкая стойка — теряешь равновесие", "Переднее колено внутрь"),
            contra = listOf("Острая боль в колене"), harder = "lu_bulgarian",
        ),
        Exercise(
            "lu_reverse", "Обратные выпады", Slot.LUNGE, listOf(QUADS, GLUTES), listOf(HAMSTRINGS), difficulty = 2, perSide = true,
            cues = listOf("Шаг назад, оба колена ~90°", "Отталкивайся передней пяткой"),
            mistakes = listOf("Удар коленом об пол", "Наклон корпуса вбок"),
            contra = listOf("Острая боль в колене"), harder = "lu_bulgarian",
        ),
        Exercise(
            "lu_stepup", "Зашагивания на возвышение", Slot.LUNGE, listOf(QUADS, GLUTES), listOf(CALVES), difficulty = 2, perSide = true,
            cues = listOf("Только устойчивая опора (ступенька, лавка)", "Толкай ногой на опоре, не подпрыгивай второй"),
            mistakes = listOf("Отталкиваться задней ногой", "Шаткая опора"),
            contra = listOf("Проблемы с равновесием — держись за перила"), harder = "lu_bulgarian",
        ),
        Exercise(
            "lu_lateral", "Боковые выпады", Slot.LUNGE, listOf(QUADS, GLUTES), listOf(HAMSTRINGS), difficulty = 2, perSide = true,
            cues = listOf("Широкий шаг в сторону, таз назад", "Вторая нога прямая, стопы на полу"),
            mistakes = listOf("Колено уходит за носок внутрь"),
            contra = listOf("Боль в паху или колене"), harder = "lu_bulgarian",
        ),
        Exercise(
            "lu_bulgarian", "Болгарские выпады", Slot.LUNGE, listOf(QUADS, GLUTES), listOf(HAMSTRINGS, CORE), difficulty = 3, perSide = true,
            cues = listOf("Задняя стопа на стуле, передняя далеко впереди", "Опускайся вертикально, контроль в нижней точке"),
            mistakes = listOf("Слишком близко к стулу", "Наваливаться на заднюю ногу"),
            contra = listOf("Острая боль в колене"), easier = "lu_split",
        ),
        Exercise(
            "lu_db_reverse", "Обратные выпады с гантелями", Slot.LUNGE, listOf(QUADS, GLUTES), listOf(FOREARMS, CORE), equipment = DUMBBELLS, difficulty = 3, perSide = true,
            cues = listOf("Гантели в опущенных руках, плечи назад", "Контролируемый шаг назад"),
            mistakes = listOf("Раскачивание гантелей"),
            contra = listOf("Острая боль в колене"), easier = "lu_reverse",
        ),
        // ---------- HORIZONTAL PUSH ----------
        Exercise(
            "hp_wall", "Отжимания от стены", Slot.HPUSH, listOf(CHEST), listOf(TRICEPS, SHOULDERS), difficulty = 1,
            cues = listOf("Тело — прямая линия от пяток до головы", "Локти под углом ~45° к корпусу"),
            mistakes = listOf("Провисание таза", "Локти в стороны на 90°"),
            contra = listOf("Острая боль в плече или запястье"), harder = "hp_incline",
        ),
        Exercise(
            "hp_incline", "Отжимания от опоры (стол, лавка)", Slot.HPUSH, listOf(CHEST), listOf(TRICEPS, SHOULDERS, CORE), difficulty = 2,
            cues = listOf("Чем ниже опора — тем тяжелее", "Грудь к краю опоры, полная амплитуда"),
            mistakes = listOf("Короткая амплитуда", "Голова тянется вперёд"),
            contra = listOf("Острая боль в плече или запястье"), easier = "hp_wall", harder = "hp_push",
        ),
        Exercise(
            "hp_push", "Отжимания от пола", Slot.HPUSH, listOf(CHEST), listOf(TRICEPS, SHOULDERS, CORE), difficulty = 3,
            cues = listOf("Ладони чуть шире плеч, пресс и ягодицы напряжены", "Опускайся до 5–10 см от пола, локти ~45°"),
            mistakes = listOf("Провис в пояснице", "Полуамплитуда"),
            contra = listOf("Боль в запястьях — используй упоры или кулаки"), easier = "hp_incline", harder = "hp_decline",
        ),
        Exercise(
            "hp_decline", "Отжимания с ногами на возвышении", Slot.HPUSH, listOf(CHEST, SHOULDERS), listOf(TRICEPS, CORE), difficulty = 4,
            cues = listOf("Стопы на стуле, корпус прямой", "Контролируй опускание 2–3 с"),
            mistakes = listOf("Ломаться в тазу"),
            contra = listOf("Боль в плечах"), easier = "hp_push", harder = "hp_archer",
        ),
        Exercise(
            "hp_archer", "Отжимания «лучник»", Slot.HPUSH, listOf(CHEST), listOf(TRICEPS, SHOULDERS, CORE), difficulty = 5, perSide = true,
            cues = listOf("Широкая постановка, вес смещается на одну руку", "Вторая рука почти прямая и только помогает"),
            mistakes = listOf("Разворот корпуса"),
            contra = listOf("Боль в плече или локте"), easier = "hp_decline",
        ),
        Exercise(
            "hp_db_floor", "Жим гантелей лёжа на полу", Slot.HPUSH, listOf(CHEST), listOf(TRICEPS, SHOULDERS), equipment = DUMBBELLS, difficulty = 2,
            cues = listOf("Плечи прижаты к полу, локти под ~45°", "Плечо касается пола — и вверх"),
            mistakes = listOf("Бросать локти на пол"),
            contra = listOf("Острая боль в плече"), harder = "hp_push",
        ),
        // ---------- VERTICAL PUSH ----------
        Exercise(
            "vp_pike_inc", "Отжимания уголком от опоры", Slot.VPUSH, listOf(SHOULDERS), listOf(TRICEPS, UPPER_BACK), difficulty = 2,
            cues = listOf("Руки на лавке, таз высоко, тело буквой «Λ»", "Голова опускается перед руками"),
            mistakes = listOf("Превращать в обычные отжимания"),
            contra = listOf("Повышенное давление, глаукома — без положений вниз головой", "Боль в плече"), harder = "vp_pike",
        ),
        Exercise(
            "vp_pike", "Отжимания уголком (пайк)", Slot.VPUSH, listOf(SHOULDERS), listOf(TRICEPS, UPPER_BACK), difficulty = 3,
            cues = listOf("Руки и стопы на полу, таз максимально вверх", "Лоб к точке перед ладонями, локти назад-в стороны"),
            mistakes = listOf("Сгибать спину вместо работы плеч"),
            contra = listOf("Повышенное давление, глаукома", "Боль в плече"), easier = "vp_pike_inc", harder = "vp_pike_elev",
        ),
        Exercise(
            "vp_pike_elev", "Пайк-отжимания с ногами на возвышении", Slot.VPUSH, listOf(SHOULDERS), listOf(TRICEPS, UPPER_BACK, CORE), difficulty = 4,
            cues = listOf("Стопы на стуле, корпус почти вертикален", "Медленно вниз, мощно вверх"),
            mistakes = listOf("Падение на голову — опускайся под контролем"),
            contra = listOf("Повышенное давление, глаукома, проблемы с шеей"), easier = "vp_pike",
        ),
        Exercise(
            "vp_db_press", "Жим гантелей вверх стоя", Slot.VPUSH, listOf(SHOULDERS), listOf(TRICEPS, CORE), equipment = DUMBBELLS, difficulty = 3,
            cues = listOf("Ягодицы и пресс напряжены, не прогибайся", "Гантели вверх и чуть назад, над ушами"),
            mistakes = listOf("Прогиб в пояснице", "Подталкивать ногами"),
            contra = listOf("Боль в плече при подъёме руки"), easier = "vp_pike_inc",
        ),
        Exercise(
            "vp_band_press", "Жим резинки вверх", Slot.VPUSH, listOf(SHOULDERS), listOf(TRICEPS), equipment = BAND, difficulty = 2,
            cues = listOf("Резинка под стопами, руки от плеч вверх", "Контроль на опускании"),
            mistakes = listOf("Рывки, прогиб"),
            contra = listOf("Боль в плече"), harder = "vp_pike",
        ),
        // ---------- HORIZONTAL PULL ----------
        Exercise(
            "pl_ytw", "Подъёмы рук Y-T-W лёжа", Slot.HPULL, listOf(UPPER_BACK), listOf(SHOULDERS, LOWER_BACK), difficulty = 1,
            cues = listOf("Лёжа на животе, лоб на полотенце", "Подними руки буквой Y, затем T, затем W, лопатки вниз и вместе"),
            mistakes = listOf("Задирать голову", "Поднимать плечи к ушам"),
            contra = listOf("Острая боль в плече"), harder = "pl_table_row",
        ),
        Exercise(
            "pl_table_row", "Тяга под столом (австралийские подтягивания)", Slot.HPULL, listOf(UPPER_BACK, LATS), listOf(BICEPS, FOREARMS), difficulty = 2,
            cues = listOf("Только ОЧЕНЬ прочный стол — проверь, что не опрокинется", "Тело прямое, тянись грудью к краю, лопатки вместе"),
            mistakes = listOf("Провисание таза", "Тянуть шеей"),
            contra = listOf("Хлипкая мебель — замени на тягу резинки или гантели"), easier = "pl_ytw",
        ),
        Exercise(
            "pl_db_row", "Тяга гантели в наклоне одной рукой", Slot.HPULL, listOf(LATS, UPPER_BACK), listOf(BICEPS, FOREARMS), equipment = DUMBBELLS, difficulty = 2, perSide = true,
            cues = listOf("Опора рукой и коленом на лавку, спина ровная", "Тяни локоть к бедру, а не к плечу"),
            mistakes = listOf("Скручивать корпус", "Рывком"),
            contra = listOf("Острая боль в пояснице"),
        ),
        Exercise(
            "pl_band_row", "Тяга резинки к поясу", Slot.HPULL, listOf(UPPER_BACK, LATS), listOf(BICEPS), equipment = BAND, difficulty = 1,
            cues = listOf("Резинка за опорой на уровне груди", "Сводишь лопатки, локти вдоль корпуса"),
            mistakes = listOf("Отклоняться назад корпусом"),
            contra = listOf("Надорванная резинка — замени"),
        ),
        // ---------- VERTICAL PULL ----------
        Exercise(
            "vl_superman", "«Супермен» с тягой локтей", Slot.VPULL, listOf(LATS, UPPER_BACK), listOf(LOWER_BACK), difficulty = 1,
            cues = listOf("Лёжа на животе, руки вперёд", "Тяни локти вниз к рёбрам, как в подтягивании, и снова вперёд"),
            mistakes = listOf("Сильно запрокидывать голову"),
            contra = listOf("Острая боль в пояснице"),
        ),
        Exercise(
            "vl_band_pulldown", "Тяга резинки сверху", Slot.VPULL, listOf(LATS), listOf(BICEPS, UPPER_BACK), equipment = BAND, difficulty = 2,
            cues = listOf("Резинка закреплена высоко (дверь, турник)", "Тяни локти вниз к рёбрам, грудь вперёд"),
            mistakes = listOf("Работа только руками"),
            contra = listOf("Ненадёжное крепление резинки"),
        ),
        Exercise(
            "vl_hang", "Вис на турнике с активными лопатками", Slot.VPULL, listOf(LATS, FOREARMS), listOf(UPPER_BACK, SHOULDERS), equipment = BAR, difficulty = 1, timed = true,
            cues = listOf("Хват сверху, плечи «вниз от ушей»", "Можно стопами на опоре, если тяжело"),
            mistakes = listOf("Висеть «на связках» с плечами у ушей"),
            contra = listOf("Нестабильность плеча, свежая травма"), harder = "vl_negative",
        ),
        Exercise(
            "vl_negative", "Негативные подтягивания", Slot.VPULL, listOf(LATS), listOf(BICEPS, UPPER_BACK, FOREARMS), equipment = BAR, difficulty = 3,
            cues = listOf("Запрыгни или встань на опору в верхнюю точку", "Опускайся 3–5 секунд до прямых рук"),
            mistakes = listOf("Бросить себя вниз"),
            contra = listOf("Боль в локте или плече"), easier = "vl_hang", harder = "vl_pullup",
        ),
        Exercise(
            "vl_pullup", "Подтягивания", Slot.VPULL, listOf(LATS), listOf(BICEPS, UPPER_BACK, FOREARMS, CORE), equipment = BAR, difficulty = 4,
            cues = listOf("Начинай с лопаток, тянись грудью к перекладине", "Полная амплитуда без раскачки"),
            mistakes = listOf("Кипинг/раскачка", "Подбородок тянется вперёд вместо груди"),
            contra = listOf("Боль в локте или плече"), easier = "vl_negative",
        ),
        // ---------- CORE ----------
        Exercise(
            "co_deadbug", "«Мёртвый жук»", Slot.CORE, listOf(CORE), difficulty = 1, perSide = true,
            cues = listOf("Поясница прижата к полу всё время", "Медленно разгибай противоположные руку и ногу, выдох"),
            mistakes = listOf("Поясница отрывается от пола"),
            contra = listOf("Острая боль в спине"), harder = "co_hollow",
        ),
        Exercise(
            "co_birddog", "«Птица-собака»", Slot.CORE, listOf(CORE, LOWER_BACK), listOf(GLUTES), difficulty = 1, perSide = true,
            cues = listOf("На четвереньках вытяни противоположные руку и ногу", "Таз не качается, держи 2 секунды"),
            mistakes = listOf("Прогиб в пояснице"),
            contra = listOf("Боль в запястьях или коленях — подложи коврик"),
        ),
        Exercise(
            "co_plank", "Планка", Slot.CORE, listOf(CORE), listOf(SHOULDERS), difficulty = 2, timed = true,
            cues = listOf("Локти под плечами, тело прямое", "Ягодицы напряжены, рёбра к тазу"),
            mistakes = listOf("Провисший таз", "Задержка дыхания"),
            contra = listOf("Повышенное давление — дыши, не задерживай дыхание"), harder = "co_hollow",
        ),
        Exercise(
            "co_side_plank", "Боковая планка", Slot.CORE, listOf(CORE), listOf(GLUTES, SHOULDERS), difficulty = 2, timed = true, perSide = true,
            cues = listOf("Локоть под плечом, тело в одну линию", "Можно с колен для облегчения"),
            mistakes = listOf("Таз проваливается"),
            contra = listOf("Боль в плече"),
        ),
        Exercise(
            "co_reverse_crunch", "Обратные скручивания", Slot.CORE, listOf(CORE), difficulty = 2,
            cues = listOf("Лёжа на спине, колени к груди, подкручивай таз", "Опускай медленно"),
            mistakes = listOf("Махи ногами"),
            contra = listOf("Острая боль в пояснице"),
        ),
        Exercise(
            "co_hollow", "Лодочка (hollow hold)", Slot.CORE, listOf(CORE), difficulty = 3, timed = true,
            cues = listOf("Поясница прижата, лопатки и ноги приподняты", "Чем дальше руки и ноги — тем тяжелее"),
            mistakes = listOf("Прогиб в пояснице"),
            contra = listOf("Боль в спине — согни колени"), easier = "co_deadbug",
        ),
        Exercise(
            "co_pallof", "Жим Паллофа с резинкой", Slot.CORE, listOf(CORE), listOf(SHOULDERS), equipment = BAND, difficulty = 2, perSide = true,
            cues = listOf("Резинка сбоку на уровне груди", "Выпрями руки вперёд и не дай корпусу развернуться"),
            mistakes = listOf("Поворот корпуса к резинке"),
            contra = listOf("—"),
        ),
        Exercise(
            "co_hanging_knee", "Подъёмы коленей в висе", Slot.CORE, listOf(CORE), listOf(FOREARMS, LATS), equipment = BAR, difficulty = 3,
            cues = listOf("Подкручивай таз, колени к груди", "Без раскачки"),
            mistakes = listOf("Мах ногами за счёт инерции"),
            contra = listOf("Боль в плечах"),
        ),
        // ---------- CALF ----------
        Exercise(
            "ca_raise", "Подъёмы на носки", Slot.CALF, listOf(CALVES), difficulty = 1,
            cues = listOf("Носки на краю ступеньки, пятки опускаются ниже", "Пауза наверху 1 с"),
            mistakes = listOf("Пружинить без амплитуды"),
            contra = listOf("Травма ахиллова сухожилия"), harder = "ca_single",
        ),
        Exercise(
            "ca_single", "Подъёмы на носок одной ноги", Slot.CALF, listOf(CALVES), difficulty = 3, perSide = true,
            cues = listOf("Держись за стену для баланса", "Полная амплитуда, медленно вниз"),
            mistakes = listOf("Скорость вместо амплитуды"),
            contra = listOf("Травма ахиллова сухожилия"), easier = "ca_raise",
        ),
        // ---------- ARMS ----------
        Exercise(
            "ar_towel_curl", "Изометрическое сгибание с полотенцем", Slot.BICEPS, listOf(BICEPS), listOf(FOREARMS), difficulty = 1, timed = true,
            cues = listOf("Наступи на полотенце, тяни концы вверх согнутыми руками", "Напряжение 70–80 %, дыши"),
            mistakes = listOf("Задержка дыхания"),
            contra = listOf("Повышенное давление — умеренное усилие"),
        ),
        Exercise(
            "ar_db_curl", "Сгибание рук с гантелями", Slot.BICEPS, listOf(BICEPS), listOf(FOREARMS), equipment = DUMBBELLS, difficulty = 2,
            cues = listOf("Локти прижаты к корпусу", "Опускай медленно до прямых рук"),
            mistakes = listOf("Раскачка корпусом"),
            contra = listOf("Боль в локте"),
        ),
        Exercise(
            "ar_band_curl", "Сгибание рук с резинкой", Slot.BICEPS, listOf(BICEPS), listOf(FOREARMS), equipment = BAND, difficulty = 1,
            cues = listOf("Резинка под стопами", "Локти неподвижны"),
            mistakes = listOf("Рывки"),
            contra = listOf("Боль в локте"),
        ),
        Exercise(
            "ar_diamond", "Отжимания узким хватом", Slot.TRICEPS, listOf(TRICEPS), listOf(CHEST, SHOULDERS), difficulty = 3,
            cues = listOf("Ладони под грудью близко, локти вдоль корпуса", "Можно с колен или от опоры"),
            mistakes = listOf("Локти в стороны"),
            contra = listOf("Боль в запястьях или локтях"),
        ),
        Exercise(
            "ar_bench_dip", "Обратные отжимания от стула", Slot.TRICEPS, listOf(TRICEPS), listOf(SHOULDERS, CHEST), difficulty = 2,
            cues = listOf("Устойчивый стул, таз близко к сиденью", "Опускайся только до 90° в локтях"),
            mistakes = listOf("Слишком глубоко — нагрузка на переднюю часть плеча"),
            contra = listOf("Боль в передней части плеча — пропусти"),
        ),
        Exercise(
            "ar_db_ext", "Разгибание гантели из-за головы", Slot.TRICEPS, listOf(TRICEPS), equipment = DUMBBELLS, difficulty = 2,
            cues = listOf("Локти смотрят вперёд-вверх", "Пресс напряжён, не прогибайся"),
            mistakes = listOf("Локти расходятся в стороны"),
            contra = listOf("Боль в локте или плече"),
        ),
        // ---------- GRIP ----------
        Exercise(
            "gr_towel", "Отжим полотенца и сжатия кисти", Slot.GRIP, listOf(FOREARMS), difficulty = 1, timed = true,
            cues = listOf("Скручивай мокрое или сухое полотенце в обе стороны", "Сильный хват помогает освобождаться от захватов"),
            mistakes = listOf("Рывки кистью"),
            contra = listOf("Туннельный синдром — умеренно"),
        ),
        Exercise(
            "gr_farmer", "Фермерская прогулка", Slot.GRIP, listOf(FOREARMS, UPPER_BACK), listOf(CORE, CALVES), equipment = DUMBBELLS, difficulty = 2, timed = true,
            cues = listOf("Тяжёлые гантели (или сумки с водой) в руках", "Ходи ровно, плечи назад, короткие шаги"),
            mistakes = listOf("Сутулиться"),
            contra = listOf("Острая боль в спине"),
        ),
        Exercise(
            "gr_hang", "Вис на время", Slot.GRIP, listOf(FOREARMS), listOf(LATS), equipment = BAR, difficulty = 2, timed = true,
            cues = listOf("Активные плечи, дыши ровно"),
            mistakes = listOf("Расслабленные плечи у ушей"),
            contra = listOf("Нестабильность плеча"),
        ),
        // ---------- NECK ----------
        Exercise(
            "ne_iso", "Изометрия шеи ладонью (4 направления)", Slot.NECK, listOf(NECK), difficulty = 1, timed = true,
            cues = listOf("Ладонь на лоб, затылок, виски — давишь головой в руку, голова не двигается", "Усилие мягкое, 30–50 %, по 10 с в каждую сторону"),
            mistakes = listOf("Резкие толчки и вращения головой"),
            contra = listOf("Травмы или грыжи шейного отдела — только с врачом", "Головокружение при нагрузке"),
        ),
        // ---------- CONDITIONING ----------
        Exercise(
            "cd_jacks", "Прыжки «звёздочка»", Slot.CONDITIONING, listOf(CALVES, SHOULDERS), listOf(QUADS), difficulty = 1, timed = true,
            cues = listOf("Мягкое приземление на носки", "Без прыжков: шаг в сторону с подъёмом рук"),
            mistakes = listOf("Жёсткое приземление на пятки"),
            contra = listOf("Проблемы с коленями — вариант с шагом"),
        ),
        Exercise(
            "cd_climbers", "Скалолаз", Slot.CONDITIONING, listOf(CORE), listOf(SHOULDERS, QUADS), difficulty = 2, timed = true,
            cues = listOf("Упор лёжа, быстро подтягивай колени к груди по очереди", "Таз не задирай"),
            mistakes = listOf("Таз выше плеч"),
            contra = listOf("Боль в запястьях"),
        ),
        Exercise(
            "cd_burpee", "Бёрпи", Slot.CONDITIONING, listOf(QUADS, CHEST), listOf(CORE, SHOULDERS), difficulty = 3, timed = true,
            cues = listOf("Присед — упор лёжа — обратно — выпрыгивание", "Облегчённо: без отжимания и прыжка"),
            mistakes = listOf("Провисание в упоре"),
            contra = listOf("Повышенное давление, болезни сердца — только с разрешения врача"),
        ),
        Exercise(
            "cd_shadow", "Бой с тенью (интервалы)", Slot.CONDITIONING, listOf(SHOULDERS, CORE), listOf(CALVES), difficulty = 2, timed = true,
            cues = listOf("Стойка, руки у подбородка, двигайся и наноси лёгкие удары в воздух", "Не выпрямляй локти до щелчка"),
            mistakes = listOf("Опускать руки", "Бить «в замок» локтя"),
            contra = listOf("Боль в локтях"),
        ),
        Exercise(
            "cd_sprint", "Спринт-интервалы (улица)", Slot.CONDITIONING, listOf(QUADS, HAMSTRINGS, GLUTES), listOf(CALVES), difficulty = 3, timed = true,
            cues = listOf("Бег 10–15 с на 80–90 %, затем шаг до восстановления", "Хорошая разминка обязательна", "Умение быстро убежать — главный навык самообороны"),
            mistakes = listOf("Спринт без разминки — риск травмы бедра"),
            contra = listOf("Болезни сердца, травмы ног — замени на быструю ходьбу в горку"),
        ),
    )

    private val byId = all.associateBy { it.id }
    fun get(id: String): Exercise = byId[id] ?: error("Unknown exercise $id")
    fun find(id: String): Exercise? = byId[id]
}
