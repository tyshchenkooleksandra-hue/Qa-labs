# Coverage Report

## Загальне покриття

### Frontend (JavaScript — Jest + Istanbul)

| Метрика      | Покриття |
|--------------|----------|
| Statements   | 30.16%   |
| Branches     | 9.26%    |
| Functions    | 8.75%    |
| Lines        | 32.07%   |

> Запуск: `npm test -- --watchAll=false --coverage` (35 test suites, 164 тести)

### Backend (Java — JUnit 5 + JaCoCo)

| Метрика              | Покриття |
|----------------------|----------|
| Instructions (байт-код) | 24.4%  |
| Branches             | 17.6%    |
| Methods              | 27.3%    |
| Lines                | 21.5%    |

> Запуск: `./gradlew test jacocoTestReport`

---

## Покриття файлів Варіанту 3 (наші тести)

### `src/helper/getHref.js`
| Statements | Branches | Functions | Lines |
|------------|----------|-----------|-------|
| 100%       | 100%     | 100%      | 100%  |

### `src/utils/sheduleUtils.js`
| Statements | Branches | Functions | Lines |
|------------|----------|-----------|-------|
| 100%       | 100%     | 100%      | 100%  |

### `com.softserve.service.impl.DepartmentServiceImpl`
| Methods покрито |
|----------------|
| 100%           |

### `com.softserve.service.impl.RoomServiceImpl`
| Methods покрито |
|----------------|
| 63.2%          |

---

## Аналіз

### Які функції/класи покриті найкраще?

**Frontend:**
- `src/helper/getHref.js` — **100%** по всіх метриках завдяки 8 написаним тестам, що перевіряють рендеринг посилань, атрибути (`target`, `rel`, `className`) та граничні випадки (null, порожній рядок).
- `src/utils/sheduleUtils.js` — **100%** по всіх метриках завдяки 13 тестам для `isNotReadySchedule`, `filterClassesArray`, `getScheduleByType`.
- `src/utils/formUtils.js`, `selectUtils.js`, `sortStrings.js`, `urlUtils.js` — **100%** (мають власні unit-тести).
- `src/share/Snackbar/SnackbarComponent.js` — **100% statements/functions**, тому що компонент простий і вже охоплений наявними тестами.

**Backend:**
- `com/softserve/entity/enums`, `com/softserve/dto/enums` — **100%** (прості enum-класи, не потребують логіки).
- `com/softserve/entity` — **89.5%** (entity-класи покриваються побічно через сервісні тести, getter/setter перевіряються неявно).
- `DepartmentServiceImpl`, `GroupServiceImpl`, `SubjectServiceImpl`, `RoomTypeServiceImpl` — **100% методів** завдяки розширеним `@Nested`-тестам.

### Які потребують додаткових тестів?

**Frontend:**
- `src/components/` — 0–5% покриття. Більшість React-компонентів (`Schedule`, `Lesson`, `Teacher`, форми) не мають тестів взагалі. Потребують інтеграційних тестів із `@testing-library/react`.
- `src/sagas/` — ~0%. Saga-файли (`watchFetchSemesters`, `watchFetchSchedule` тощо) не покриті, хоча містять складну async-логіку з race conditions.
- `src/validation/storeValidation.js` — **12.9%**. Містить 9 валідаційних функцій, але більшість не тестуються.
- `src/share/renderedFields/` — 14–50%. Поля форм (`select`, `error`, `renderMultiselect`) критичні для UX, але не тестуються.

**Backend:**
- `com/softserve/security/` — **0%**. JWT-фільтри та конфігурація безпеки не мають тестів. Це критичний ризик — помилки безпеки не виявляються автоматично.
- `com/softserve/service/impl/ScheduleServiceImpl` — **22.4% методів**. Найскладніший сервіс із бізнес-логікою розкладу, але покритий лише на чверть.
- `com/softserve/controller/` — **0%**. REST-контролери не мають `@WebMvcTest` тестів.
- `com/softserve/repository/impl/` — **0%**. Repository-реалізації не тестуються (потребують `@DataJpaTest`).

### Чому деякі branches не покриті?

**Frontend (Branches: 9.26%):**
1. **Redux-пов'язані гілки**: Більшість `if/else` знаходяться в Redux reducers, saga-handlers та компонентах, які ніколи не рендеряться в тестовому середовищі. Наприклад, `src/reducers/` містять умови для різних action types, але тести не диспатчать ці actions.
2. **Гілки обробки помилок**: Конструкції типу `catch (e) { ... }` та `if (error) return null` у компонентах недосяжні без мок-сервісів, що повертають помилки.
3. **Умови завантаження**: Патерн `if (loading) return <Spinner/>` зустрічається у десятках компонентів, але стан `loading=true` рідко симулюється в тестах.
4. **`sagaUtils.js` (60% branches)**: Гілка `if (error.response && error.response.data)` не покрита — для цього потрібно передавати мок-об'єкт без `response.data`.

**Backend (Branches: 17.6%):**
1. **Exception branches**: Методи типу `if (entity == null) throw EntityNotFoundException` покриті тільки для happy path. Наприклад, `RoomServiceImpl.getAvailableRoomsForSchedule()` (0% coverage) містить складну умовну логіку з EvenOdd/DayOfWeek, яку важко протестувати без реальної БД.
2. **Spring Security умови**: Гілки `if (userDetails.getAuthorities().contains(...))` в JWT-фільтрах не покриті взагалі (0% для `security/`).
3. **Optional chaining**: Конструкції `.orElse()`, `.orElseThrow()` — альтернативна гілка (empty Optional) не завжди тестується.
4. **`ScheduleServiceImpl`**: Містить складні вкладені умови для генерації розкладу (перевірка конфліктів, парний/непарний тиждень). Ці branches потребують комбінаторного тестування.

---

## Скріншот

> Нижче — текстовий вивід coverage-звіту з консолі (Jest):

```
---------------------------------|---------|----------|---------|---------|
File                             | % Stmts | % Branch | % Funcs | % Lines |
---------------------------------|---------|----------|---------|---------|
All files                        |   30.16 |     9.26 |    8.75 |   32.07 |
 src/helper                      |   59.25 |    44.44 |   54.54 |   59.62 |
  getHref.js                     |     100 |      100 |     100 |     100 |
 src/utils                       |   93.82 |    82.75 |   96.66 |   93.24 |
  sheduleUtils.js                |     100 |      100 |     100 |     100 |
---------------------------------|---------|----------|---------|---------|
```

> JaCoCo (Java backend) — підсумок по пакетах:

```
Package                            | Instructions | Methods
-----------------------------------|--------------|--------
com/softserve/entity/enums         |     100%     |  100%
com/softserve/dto/enums            |     100%     |  100%
com/softserve/entity               |      89.5%   |  ~85%
com/softserve/service/impl         |      52.2%   |
  DepartmentServiceImpl            |      100%    |  100%
  GroupServiceImpl                 |      100%    |  100%
  SubjectServiceImpl               |      100%    |  100%
  RoomTypeServiceImpl              |      100%    |  100%
  StudentServiceImpl               |       --     |  89.5%
  RoomServiceImpl                  |       --     |  63.2%
  ScheduleServiceImpl              |       --     |  22.4%
  SchedulePublishServiceImpl       |        0%    |    0%
com/softserve/security             |        0%    |    0%
com/softserve/controller           |        0%    |    0%
```
