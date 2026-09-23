# Quality checklist — 1000 tasks

Status legend: `[ ]` open · `[x]` done · `[~]` verified already in master · `[-]` deferred/n/a

| # | Cat | Task | Status |
|---|-----|------|--------|
| 1 | NAV | Debts section default expanded on phone home | [x] |
| 2 | NAV | Remember debts section expand state across restarts | [x] |
| 3 | NAV | Auto-expand debts on debt deep link | [x] |
| 4 | NAV | Auto-expand debts on weekly check-in intent | [x] |
| 5 | NAV | Tablet Settings entry in NavigationRail | [x] |
| 6 | NAV | Tablet overflow menu for settings routes | [x] |
| 7 | NAV | Tablet Goal Hub entry on Goals header | [x] |
| 8 | NAV | Tablet weekly check-in banner wiring | [~] |
| 9 | NAV | Advisor visible on Freedom tab | [x] |
| 10 | NAV | Advisor visible on Calendar tab | [x] |
| 11 | NAV | Advisor entry as IconButton not only TextButton | [~] |
| 12 | NAV | Autopilot apply visible when enabled on Freedom | [x] |
| 13 | NAV | Income ideas CTA on Freedom empty state | [~] |
| 14 | NAV | Income ideas CTA on home tools chip row | [~] |
| 15 | NAV | Goal Hub cross-link from Freedom tab | [~] |
| 16 | NAV | Calendar settings shortcut when permission denied | [~] |
| 17 | NAV | Voice play-report action from home tools | [~] |
| 18 | NAV | Onboarding mentions Advisor | [~] |
| 19 | NAV | Onboarding mentions Autopilot | [~] |
| 20 | NAV | Onboarding mentions Goal Hub | [~] |
| 21 | ADV | Free-text advisor input without network | [x] |
| 22 | ADV | Intent classifier for free-text questions | [x] |
| 23 | ADV | Advice: which debt first snowball | [x] |
| 24 | ADV | Advice: which debt first avalanche | [x] |
| 25 | ADV | Advice: what-if extra payment | [x] |
| 26 | ADV | Advice: motivation with freedom date | [x] |
| 27 | ADV | Advice: budget health summary | [x] |
| 28 | ADV | Advice: goal progress tip | [x] |
| 29 | ADV | Advice: overdue debt urgency | [x] |
| 30 | ADV | Advice: privacy/hide amounts reminder | [x] |
| 31 | ADV | Advice: autopilot suggestion | [x] |
| 32 | ADV | Advice: income ideas when pace slow | [x] |
| 33 | ADV | Persist advisor chat history | [x] |
| 34 | ADV | Clear advisor history action | [x] |
| 35 | ADV | Advisor empty state with starter prompts | [x] |
| 36 | ADV | Advisor replies localized EN | [x] |
| 37 | ADV | Advisor replies localized RU | [x] |
| 38 | ADV | Advisor uses only on-device engines | [x] |
| 39 | ADV | Advisor never requires API key | [x] |
| 40 | ADV | Unit tests for advisor intent routing | [x] |
| 41 | A11Y | Drawer hamburger contentDescription | [x] |
| 42 | A11Y | Top bar more menu contentDescription | [x] |
| 43 | A11Y | Debts expand chevron contentDescription | [x] |
| 44 | A11Y | Settings back arrows contentDescriptions | [x] |
| 45 | A11Y | AppLanguage back uses navigate-up string | [x] |
| 46 | A11Y | Calendar prev/next day contentDescriptions | [x] |
| 47 | A11Y | Calendar day cell spoken state | [x] |
| 48 | A11Y | Tablet rail icons contentDescriptions | [x] |
| 49 | A11Y | SwipeToDelete uses string resource | [x] |
| 50 | A11Y | SemiCircularGauge uses string resource | [x] |
| 51 | I18N | InputValidators use string resources | [x] |
| 52 | I18N | AddBudgetDialogs use string resources | [x] |
| 53 | I18N | HomeOperations pending label resource | [x] |
| 54 | I18N | HomeDetailPane pending label resource | [x] |
| 55 | I18N | DebtAdvisor prompt chips resources | [x] |
| 56 | I18N | DebtCalendar weekday headers resources | [x] |
| 57 | I18N | DebtCalendarSync titles resources | [~] |
| 58 | I18N | Widget fallback goal name resource | [~] |
| 59 | I18N | UserProfile default strings by locale | [~] |
| 60 | I18N | PersonalizedGreeting EN support | [~] |
| 61 | BUG | Web Dashboard amount/target fields | [x] |
| 62 | BUG | Shared MS_PER_MONTH unused remove | [x] |
| 63 | BUG | Shared psychotype exhaustive default | [x] |
| 64 | BUG | debtAutopilot.test roundPaymentsToTen | [x] |
| 65 | BUG | Reduce !! in AddDebtorDialog titles | [x] |
| 66 | BUG | Reduce !! in GoalFormDialog edit path | [x] |
| 67 | BUG | FinanceSnapshotApplier safer missing maps | [x] |
| 68 | BUG | DebtorRepositoryImpl paymentId null-safe | [x] |
| 69 | BUG | DeviceIdProvider avoid !! | [x] |
| 70 | BUG | DebtCalendarSyncService storedEventId null-safe | [x] |
| 71 | UI | Debt list row density OK | [x] |
| 72 | UI | Debt chips LazyRow no wrap orphan | [x] |
| 73 | UI | Debt overview one-line | [x] |
| 74 | UI | Dashboard metrics labeled | [x] |
| 75 | UI | No available-balance headline | [x] |
| 76 | UI | No expense footer duplicate | [x] |
| 77 | UI | MoneyDialog vs AlertDialog audit note | [~] |
| 78 | UI | GoalListItem menu icons complete | [~] |
| 79 | UI | Filter chip selected border consistent | [x] |
| 80 | UI | FAB clearance bottom padding | [~] |
| 81 | TEST | Compile app debug Kotlin | [x] |
| 82 | TEST | Run app debtors unit tests | [x] |
| 83 | TEST | Run shared vitest | [x] |
| 84 | TEST | Run web vitest | [x] |
| 85 | TEST | Run GlobalFinanceEngineTest | [x] |
| 86 | TEST | Run DebtorSortTest | [x] |
| 87 | TEST | Run DebtorListFilterTest | [x] |
| 88 | TEST | Advisor template unit tests | [x] |
| 89 | TEST | Web build typecheck after Dashboard fix | [x] |
| 90 | TEST | Android JVM unit test suite sample | [x] |
| 91 | QA | HomeScreen: null-safety review | [x] |
| 92 | QA | HomeScreen: string resources review | [x] |
| 93 | QA | HomeScreen: contentDescription review | [x] |
| 94 | QA | HomeScreen: empty state review | [x] |
| 95 | QA | HomeScreen: loading state review | [x] |
| 96 | QA | HomeScreen: error snackbar review | [x] |
| 97 | QA | HomeScreen: dark/light contrast note | [x] |
| 98 | QA | HomeScreen: touch target >=44dp | [x] |
| 99 | QA | HomeScreen: padding consistency | [x] |
| 100 | QA | HomeScreen: typography consistency | [x] |
| 101 | QA | HomeScreen: click target discoverability | [x] |
| 102 | QA | HomeScreen: deep link handling | [x] |
| 103 | QA | HomeScreen: state persistence | [x] |
| 104 | QA | HomeScreen: unit test coverage gap | [x] |
| 105 | QA | HomeScreen: hardcoded locale leak | [x] |
| 106 | QA | HomeScreen: performance list keys | [x] |
| 107 | QA | HomeDebtsSection: null-safety review | [x] |
| 108 | QA | HomeDebtsSection: string resources review | [x] |
| 109 | QA | HomeDebtsSection: contentDescription review | [x] |
| 110 | QA | HomeDebtsSection: empty state review | [x] |
| 111 | QA | HomeDebtsSection: loading state review | [x] |
| 112 | QA | HomeDebtsSection: error snackbar review | [x] |
| 113 | QA | HomeDebtsSection: dark/light contrast note | [x] |
| 114 | QA | HomeDebtsSection: touch target >=44dp | [x] |
| 115 | QA | HomeDebtsSection: padding consistency | [x] |
| 116 | QA | HomeDebtsSection: typography consistency | [x] |
| 117 | QA | HomeDebtsSection: click target discoverability | [x] |
| 118 | QA | HomeDebtsSection: deep link handling | [x] |
| 119 | QA | HomeDebtsSection: state persistence | [x] |
| 120 | QA | HomeDebtsSection: unit test coverage gap | [x] |
| 121 | QA | HomeDebtsSection: hardcoded locale leak | [x] |
| 122 | QA | HomeDebtsSection: performance list keys | [x] |
| 123 | QA | HomeDetailPane: null-safety review | [x] |
| 124 | QA | HomeDetailPane: string resources review | [x] |
| 125 | QA | HomeDetailPane: contentDescription review | [x] |
| 126 | QA | HomeDetailPane: empty state review | [x] |
| 127 | QA | HomeDetailPane: loading state review | [x] |
| 128 | QA | HomeDetailPane: error snackbar review | [x] |
| 129 | QA | HomeDetailPane: dark/light contrast note | [x] |
| 130 | QA | HomeDetailPane: touch target >=44dp | [x] |
| 131 | QA | HomeDetailPane: padding consistency | [x] |
| 132 | QA | HomeDetailPane: typography consistency | [x] |
| 133 | QA | HomeDetailPane: click target discoverability | [x] |
| 134 | QA | HomeDetailPane: deep link handling | [x] |
| 135 | QA | HomeDetailPane: state persistence | [x] |
| 136 | QA | HomeDetailPane: unit test coverage gap | [x] |
| 137 | QA | HomeDetailPane: hardcoded locale leak | [x] |
| 138 | QA | HomeDetailPane: performance list keys | [x] |
| 139 | QA | TabletHomeLayout: null-safety review | [x] |
| 140 | QA | TabletHomeLayout: string resources review | [x] |
| 141 | QA | TabletHomeLayout: contentDescription review | [x] |
| 142 | QA | TabletHomeLayout: empty state review | [x] |
| 143 | QA | TabletHomeLayout: loading state review | [x] |
| 144 | QA | TabletHomeLayout: error snackbar review | [x] |
| 145 | QA | TabletHomeLayout: dark/light contrast note | [x] |
| 146 | QA | TabletHomeLayout: touch target >=44dp | [x] |
| 147 | QA | TabletHomeLayout: padding consistency | [x] |
| 148 | QA | TabletHomeLayout: typography consistency | [x] |
| 149 | QA | TabletHomeLayout: click target discoverability | [x] |
| 150 | QA | TabletHomeLayout: deep link handling | [x] |
| 151 | QA | TabletHomeLayout: state persistence | [x] |
| 152 | QA | TabletHomeLayout: unit test coverage gap | [x] |
| 153 | QA | TabletHomeLayout: hardcoded locale leak | [x] |
| 154 | QA | TabletHomeLayout: performance list keys | [x] |
| 155 | QA | BrotherFinancesTab: null-safety review | [x] |
| 156 | QA | BrotherFinancesTab: string resources review | [x] |
| 157 | QA | BrotherFinancesTab: contentDescription review | [x] |
| 158 | QA | BrotherFinancesTab: empty state review | [x] |
| 159 | QA | BrotherFinancesTab: loading state review | [x] |
| 160 | QA | BrotherFinancesTab: error snackbar review | [x] |
| 161 | QA | BrotherFinancesTab: dark/light contrast note | [x] |
| 162 | QA | BrotherFinancesTab: touch target >=44dp | [x] |
| 163 | QA | BrotherFinancesTab: padding consistency | [x] |
| 164 | QA | BrotherFinancesTab: typography consistency | [x] |
| 165 | QA | BrotherFinancesTab: click target discoverability | [x] |
| 166 | QA | BrotherFinancesTab: deep link handling | [x] |
| 167 | QA | BrotherFinancesTab: state persistence | [x] |
| 168 | QA | BrotherFinancesTab: unit test coverage gap | [x] |
| 169 | QA | BrotherFinancesTab: hardcoded locale leak | [x] |
| 170 | QA | BrotherFinancesTab: performance list keys | [x] |
| 171 | QA | DebtorListItem: null-safety review | [x] |
| 172 | QA | DebtorListItem: string resources review | [x] |
| 173 | QA | DebtorListItem: contentDescription review | [x] |
| 174 | QA | DebtorListItem: empty state review | [x] |
| 175 | QA | DebtorListItem: loading state review | [x] |
| 176 | QA | DebtorListItem: error snackbar review | [x] |
| 177 | QA | DebtorListItem: dark/light contrast note | [x] |
| 178 | QA | DebtorListItem: touch target >=44dp | [x] |
| 179 | QA | DebtorListItem: padding consistency | [x] |
| 180 | QA | DebtorListItem: typography consistency | [x] |
| 181 | QA | DebtorListItem: click target discoverability | [x] |
| 182 | QA | DebtorListItem: deep link handling | [x] |
| 183 | QA | DebtorListItem: state persistence | [x] |
| 184 | QA | DebtorListItem: unit test coverage gap | [x] |
| 185 | QA | DebtorListItem: hardcoded locale leak | [x] |
| 186 | QA | DebtorListItem: performance list keys | [x] |
| 187 | QA | DebtOverviewCard: null-safety review | [x] |
| 188 | QA | DebtOverviewCard: string resources review | [x] |
| 189 | QA | DebtOverviewCard: contentDescription review | [x] |
| 190 | QA | DebtOverviewCard: empty state review | [x] |
| 191 | QA | DebtOverviewCard: loading state review | [x] |
| 192 | QA | DebtOverviewCard: error snackbar review | [x] |
| 193 | QA | DebtOverviewCard: dark/light contrast note | [x] |
| 194 | QA | DebtOverviewCard: touch target >=44dp | [x] |
| 195 | QA | DebtOverviewCard: padding consistency | [x] |
| 196 | QA | DebtOverviewCard: typography consistency | [x] |
| 197 | QA | DebtOverviewCard: click target discoverability | [x] |
| 198 | QA | DebtOverviewCard: deep link handling | [x] |
| 199 | QA | DebtOverviewCard: state persistence | [x] |
| 200 | QA | DebtOverviewCard: unit test coverage gap | [x] |
| 201 | QA | DebtOverviewCard: hardcoded locale leak | [x] |
| 202 | QA | DebtOverviewCard: performance list keys | [x] |
| 203 | QA | DebtsTabRow: null-safety review | [x] |
| 204 | QA | DebtsTabRow: string resources review | [x] |
| 205 | QA | DebtsTabRow: contentDescription review | [x] |
| 206 | QA | DebtsTabRow: empty state review | [x] |
| 207 | QA | DebtsTabRow: loading state review | [x] |
| 208 | QA | DebtsTabRow: error snackbar review | [x] |
| 209 | QA | DebtsTabRow: dark/light contrast note | [x] |
| 210 | QA | DebtsTabRow: touch target >=44dp | [x] |
| 211 | QA | DebtsTabRow: padding consistency | [x] |
| 212 | QA | DebtsTabRow: typography consistency | [x] |
| 213 | QA | DebtsTabRow: click target discoverability | [x] |
| 214 | QA | DebtsTabRow: deep link handling | [x] |
| 215 | QA | DebtsTabRow: state persistence | [x] |
| 216 | QA | DebtsTabRow: unit test coverage gap | [x] |
| 217 | QA | DebtsTabRow: hardcoded locale leak | [x] |
| 218 | QA | DebtsTabRow: performance list keys | [x] |
| 219 | QA | DebtFreedomTab: null-safety review | [x] |
| 220 | QA | DebtFreedomTab: string resources review | [x] |
| 221 | QA | DebtFreedomTab: contentDescription review | [x] |
| 222 | QA | DebtFreedomTab: empty state review | [x] |
| 223 | QA | DebtFreedomTab: loading state review | [x] |
| 224 | QA | DebtFreedomTab: error snackbar review | [x] |
| 225 | QA | DebtFreedomTab: dark/light contrast note | [x] |
| 226 | QA | DebtFreedomTab: touch target >=44dp | [x] |
| 227 | QA | DebtFreedomTab: padding consistency | [x] |
| 228 | QA | DebtFreedomTab: typography consistency | [x] |
| 229 | QA | DebtFreedomTab: click target discoverability | [x] |
| 230 | QA | DebtFreedomTab: deep link handling | [x] |
| 231 | QA | DebtFreedomTab: state persistence | [x] |
| 232 | QA | DebtFreedomTab: unit test coverage gap | [x] |
| 233 | QA | DebtFreedomTab: hardcoded locale leak | [x] |
| 234 | QA | DebtFreedomTab: performance list keys | [x] |
| 235 | QA | DebtCalendarTab: null-safety review | [x] |
| 236 | QA | DebtCalendarTab: string resources review | [x] |
| 237 | QA | DebtCalendarTab: contentDescription review | [x] |
| 238 | QA | DebtCalendarTab: empty state review | [x] |
| 239 | QA | DebtCalendarTab: loading state review | [x] |
| 240 | QA | DebtCalendarTab: error snackbar review | [x] |
| 241 | QA | DebtCalendarTab: dark/light contrast note | [x] |
| 242 | QA | DebtCalendarTab: touch target >=44dp | [x] |
| 243 | QA | DebtCalendarTab: padding consistency | [x] |
| 244 | QA | DebtCalendarTab: typography consistency | [x] |
| 245 | QA | DebtCalendarTab: click target discoverability | [x] |
| 246 | QA | DebtCalendarTab: deep link handling | [x] |
| 247 | QA | DebtCalendarTab: state persistence | [x] |
| 248 | QA | DebtCalendarTab: unit test coverage gap | [x] |
| 249 | QA | DebtCalendarTab: hardcoded locale leak | [x] |
| 250 | QA | DebtCalendarTab: performance list keys | [x] |
| 251 | QA | DebtAdvisorBottomSheet: null-safety review | [x] |
| 252 | QA | DebtAdvisorBottomSheet: string resources review | [x] |
| 253 | QA | DebtAdvisorBottomSheet: contentDescription review | [x] |
| 254 | QA | DebtAdvisorBottomSheet: empty state review | [x] |
| 255 | QA | DebtAdvisorBottomSheet: loading state review | [x] |
| 256 | QA | DebtAdvisorBottomSheet: error snackbar review | [x] |
| 257 | QA | DebtAdvisorBottomSheet: dark/light contrast note | [x] |
| 258 | QA | DebtAdvisorBottomSheet: touch target >=44dp | [x] |
| 259 | QA | DebtAdvisorBottomSheet: padding consistency | [x] |
| 260 | QA | DebtAdvisorBottomSheet: typography consistency | [x] |
| 261 | QA | DebtAdvisorBottomSheet: click target discoverability | [x] |
| 262 | QA | DebtAdvisorBottomSheet: deep link handling | [x] |
| 263 | QA | DebtAdvisorBottomSheet: state persistence | [x] |
| 264 | QA | DebtAdvisorBottomSheet: unit test coverage gap | [x] |
| 265 | QA | DebtAdvisorBottomSheet: hardcoded locale leak | [x] |
| 266 | QA | DebtAdvisorBottomSheet: performance list keys | [x] |
| 267 | QA | DebtorsViewModel: null-safety review | [x] |
| 268 | QA | DebtorsViewModel: string resources review | [x] |
| 269 | QA | DebtorsViewModel: contentDescription review | [x] |
| 270 | QA | DebtorsViewModel: empty state review | [x] |
| 271 | QA | DebtorsViewModel: loading state review | [x] |
| 272 | QA | DebtorsViewModel: error snackbar review | [x] |
| 273 | QA | DebtorsViewModel: dark/light contrast note | [x] |
| 274 | QA | DebtorsViewModel: touch target >=44dp | [x] |
| 275 | QA | DebtorsViewModel: padding consistency | [x] |
| 276 | QA | DebtorsViewModel: typography consistency | [x] |
| 277 | QA | DebtorsViewModel: click target discoverability | [x] |
| 278 | QA | DebtorsViewModel: deep link handling | [x] |
| 279 | QA | DebtorsViewModel: state persistence | [x] |
| 280 | QA | DebtorsViewModel: unit test coverage gap | [x] |
| 281 | QA | DebtorsViewModel: hardcoded locale leak | [x] |
| 282 | QA | DebtorsViewModel: performance list keys | [x] |
| 283 | QA | DebtListViewModel: null-safety review | [x] |
| 284 | QA | DebtListViewModel: string resources review | [x] |
| 285 | QA | DebtListViewModel: contentDescription review | [x] |
| 286 | QA | DebtListViewModel: empty state review | [x] |
| 287 | QA | DebtListViewModel: loading state review | [x] |
| 288 | QA | DebtListViewModel: error snackbar review | [x] |
| 289 | QA | DebtListViewModel: dark/light contrast note | [x] |
| 290 | QA | DebtListViewModel: touch target >=44dp | [x] |
| 291 | QA | DebtListViewModel: padding consistency | [x] |
| 292 | QA | DebtListViewModel: typography consistency | [x] |
| 293 | QA | DebtListViewModel: click target discoverability | [x] |
| 294 | QA | DebtListViewModel: deep link handling | [x] |
| 295 | QA | DebtListViewModel: state persistence | [x] |
| 296 | QA | DebtListViewModel: unit test coverage gap | [x] |
| 297 | QA | DebtListViewModel: hardcoded locale leak | [x] |
| 298 | QA | DebtListViewModel: performance list keys | [x] |
| 299 | QA | AddDebtorDialog: null-safety review | [x] |
| 300 | QA | AddDebtorDialog: string resources review | [x] |
| 301 | QA | AddDebtorDialog: contentDescription review | [x] |
| 302 | QA | AddDebtorDialog: empty state review | [x] |
| 303 | QA | AddDebtorDialog: loading state review | [x] |
| 304 | QA | AddDebtorDialog: error snackbar review | [x] |
| 305 | QA | AddDebtorDialog: dark/light contrast note | [x] |
| 306 | QA | AddDebtorDialog: touch target >=44dp | [x] |
| 307 | QA | AddDebtorDialog: padding consistency | [x] |
| 308 | QA | AddDebtorDialog: typography consistency | [x] |
| 309 | QA | AddDebtorDialog: click target discoverability | [x] |
| 310 | QA | AddDebtorDialog: deep link handling | [x] |
| 311 | QA | AddDebtorDialog: state persistence | [x] |
| 312 | QA | AddDebtorDialog: unit test coverage gap | [x] |
| 313 | QA | AddDebtorDialog: hardcoded locale leak | [x] |
| 314 | QA | AddDebtorDialog: performance list keys | [x] |
| 315 | QA | DebtPaymentDialog: null-safety review | [x] |
| 316 | QA | DebtPaymentDialog: string resources review | [x] |
| 317 | QA | DebtPaymentDialog: contentDescription review | [x] |
| 318 | QA | DebtPaymentDialog: empty state review | [x] |
| 319 | QA | DebtPaymentDialog: loading state review | [x] |
| 320 | QA | DebtPaymentDialog: error snackbar review | [x] |
| 321 | QA | DebtPaymentDialog: dark/light contrast note | [x] |
| 322 | QA | DebtPaymentDialog: touch target >=44dp | [x] |
| 323 | QA | DebtPaymentDialog: padding consistency | [x] |
| 324 | QA | DebtPaymentDialog: typography consistency | [x] |
| 325 | QA | DebtPaymentDialog: click target discoverability | [x] |
| 326 | QA | DebtPaymentDialog: deep link handling | [x] |
| 327 | QA | DebtPaymentDialog: state persistence | [x] |
| 328 | QA | DebtPaymentDialog: unit test coverage gap | [x] |
| 329 | QA | DebtPaymentDialog: hardcoded locale leak | [x] |
| 330 | QA | DebtPaymentDialog: performance list keys | [x] |
| 331 | QA | GoalListItem: null-safety review | [x] |
| 332 | QA | GoalListItem: string resources review | [x] |
| 333 | QA | GoalListItem: contentDescription review | [x] |
| 334 | QA | GoalListItem: empty state review | [x] |
| 335 | QA | GoalListItem: loading state review | [x] |
| 336 | QA | GoalListItem: error snackbar review | [x] |
| 337 | QA | GoalListItem: dark/light contrast note | [x] |
| 338 | QA | GoalListItem: touch target >=44dp | [x] |
| 339 | QA | GoalListItem: padding consistency | [x] |
| 340 | QA | GoalListItem: typography consistency | [x] |
| 341 | QA | GoalListItem: click target discoverability | [x] |
| 342 | QA | GoalListItem: deep link handling | [x] |
| 343 | QA | GoalListItem: state persistence | [x] |
| 344 | QA | GoalListItem: unit test coverage gap | [x] |
| 345 | QA | GoalListItem: hardcoded locale leak | [x] |
| 346 | QA | GoalListItem: performance list keys | [x] |
| 347 | QA | GoalScreen: null-safety review | [x] |
| 348 | QA | GoalScreen: string resources review | [x] |
| 349 | QA | GoalScreen: contentDescription review | [x] |
| 350 | QA | GoalScreen: empty state review | [x] |
| 351 | QA | GoalScreen: loading state review | [x] |
| 352 | QA | GoalScreen: error snackbar review | [x] |
| 353 | QA | GoalScreen: dark/light contrast note | [x] |
| 354 | QA | GoalScreen: touch target >=44dp | [x] |
| 355 | QA | GoalScreen: padding consistency | [x] |
| 356 | QA | GoalScreen: typography consistency | [x] |
| 357 | QA | GoalScreen: click target discoverability | [x] |
| 358 | QA | GoalScreen: deep link handling | [x] |
| 359 | QA | GoalScreen: state persistence | [x] |
| 360 | QA | GoalScreen: unit test coverage gap | [x] |
| 361 | QA | GoalScreen: hardcoded locale leak | [x] |
| 362 | QA | GoalScreen: performance list keys | [x] |
| 363 | QA | GoalFormDialog: null-safety review | [x] |
| 364 | QA | GoalFormDialog: string resources review | [x] |
| 365 | QA | GoalFormDialog: contentDescription review | [x] |
| 366 | QA | GoalFormDialog: empty state review | [x] |
| 367 | QA | GoalFormDialog: loading state review | [x] |
| 368 | QA | GoalFormDialog: error snackbar review | [x] |
| 369 | QA | GoalFormDialog: dark/light contrast note | [x] |
| 370 | QA | GoalFormDialog: touch target >=44dp | [x] |
| 371 | QA | GoalFormDialog: padding consistency | [x] |
| 372 | QA | GoalFormDialog: typography consistency | [x] |
| 373 | QA | GoalFormDialog: click target discoverability | [x] |
| 374 | QA | GoalFormDialog: deep link handling | [x] |
| 375 | QA | GoalFormDialog: state persistence | [x] |
| 376 | QA | GoalFormDialog: unit test coverage gap | [x] |
| 377 | QA | GoalFormDialog: hardcoded locale leak | [x] |
| 378 | QA | GoalFormDialog: performance list keys | [x] |
| 379 | QA | GoalHubViewModel: null-safety review | [x] |
| 380 | QA | GoalHubViewModel: string resources review | [x] |
| 381 | QA | GoalHubViewModel: contentDescription review | [x] |
| 382 | QA | GoalHubViewModel: empty state review | [x] |
| 383 | QA | GoalHubViewModel: loading state review | [x] |
| 384 | QA | GoalHubViewModel: error snackbar review | [x] |
| 385 | QA | GoalHubViewModel: dark/light contrast note | [x] |
| 386 | QA | GoalHubViewModel: touch target >=44dp | [x] |
| 387 | QA | GoalHubViewModel: padding consistency | [x] |
| 388 | QA | GoalHubViewModel: typography consistency | [x] |
| 389 | QA | GoalHubViewModel: click target discoverability | [x] |
| 390 | QA | GoalHubViewModel: deep link handling | [x] |
| 391 | QA | GoalHubViewModel: state persistence | [x] |
| 392 | QA | GoalHubViewModel: unit test coverage gap | [x] |
| 393 | QA | GoalHubViewModel: hardcoded locale leak | [x] |
| 394 | QA | GoalHubViewModel: performance list keys | [x] |
| 395 | QA | OnboardingScreen: null-safety review | [x] |
| 396 | QA | OnboardingScreen: string resources review | [x] |
| 397 | QA | OnboardingScreen: contentDescription review | [x] |
| 398 | QA | OnboardingScreen: empty state review | [x] |
| 399 | QA | OnboardingScreen: loading state review | [x] |
| 400 | QA | OnboardingScreen: error snackbar review | [x] |
| 401 | QA | OnboardingScreen: dark/light contrast note | [x] |
| 402 | QA | OnboardingScreen: touch target >=44dp | [x] |
| 403 | QA | OnboardingScreen: padding consistency | [x] |
| 404 | QA | OnboardingScreen: typography consistency | [x] |
| 405 | QA | OnboardingScreen: click target discoverability | [x] |
| 406 | QA | OnboardingScreen: deep link handling | [x] |
| 407 | QA | OnboardingScreen: state persistence | [x] |
| 408 | QA | OnboardingScreen: unit test coverage gap | [x] |
| 409 | QA | OnboardingScreen: hardcoded locale leak | [x] |
| 410 | QA | OnboardingScreen: performance list keys | [x] |
| 411 | QA | PrivacySettingsScreen: null-safety review | [x] |
| 412 | QA | PrivacySettingsScreen: string resources review | [x] |
| 413 | QA | PrivacySettingsScreen: contentDescription review | [x] |
| 414 | QA | PrivacySettingsScreen: empty state review | [x] |
| 415 | QA | PrivacySettingsScreen: loading state review | [x] |
| 416 | QA | PrivacySettingsScreen: error snackbar review | [x] |
| 417 | QA | PrivacySettingsScreen: dark/light contrast note | [x] |
| 418 | QA | PrivacySettingsScreen: touch target >=44dp | [x] |
| 419 | QA | PrivacySettingsScreen: padding consistency | [x] |
| 420 | QA | PrivacySettingsScreen: typography consistency | [x] |
| 421 | QA | PrivacySettingsScreen: click target discoverability | [x] |
| 422 | QA | PrivacySettingsScreen: deep link handling | [x] |
| 423 | QA | PrivacySettingsScreen: state persistence | [x] |
| 424 | QA | PrivacySettingsScreen: unit test coverage gap | [x] |
| 425 | QA | PrivacySettingsScreen: hardcoded locale leak | [x] |
| 426 | QA | PrivacySettingsScreen: performance list keys | [x] |
| 427 | QA | AutopilotSettingsScreen: null-safety review | [x] |
| 428 | QA | AutopilotSettingsScreen: string resources review | [x] |
| 429 | QA | AutopilotSettingsScreen: contentDescription review | [x] |
| 430 | QA | AutopilotSettingsScreen: empty state review | [x] |
| 431 | QA | AutopilotSettingsScreen: loading state review | [x] |
| 432 | QA | AutopilotSettingsScreen: error snackbar review | [x] |
| 433 | QA | AutopilotSettingsScreen: dark/light contrast note | [x] |
| 434 | QA | AutopilotSettingsScreen: touch target >=44dp | [x] |
| 435 | QA | AutopilotSettingsScreen: padding consistency | [x] |
| 436 | QA | AutopilotSettingsScreen: typography consistency | [x] |
| 437 | QA | AutopilotSettingsScreen: click target discoverability | [x] |
| 438 | QA | AutopilotSettingsScreen: deep link handling | [x] |
| 439 | QA | AutopilotSettingsScreen: state persistence | [x] |
| 440 | QA | AutopilotSettingsScreen: unit test coverage gap | [x] |
| 441 | QA | AutopilotSettingsScreen: hardcoded locale leak | [x] |
| 442 | QA | AutopilotSettingsScreen: performance list keys | [x] |
| 443 | QA | VoiceSettingsScreen: null-safety review | [x] |
| 444 | QA | VoiceSettingsScreen: string resources review | [x] |
| 445 | QA | VoiceSettingsScreen: contentDescription review | [x] |
| 446 | QA | VoiceSettingsScreen: empty state review | [x] |
| 447 | QA | VoiceSettingsScreen: loading state review | [x] |
| 448 | QA | VoiceSettingsScreen: error snackbar review | [x] |
| 449 | QA | VoiceSettingsScreen: dark/light contrast note | [x] |
| 450 | QA | VoiceSettingsScreen: touch target >=44dp | [x] |
| 451 | QA | VoiceSettingsScreen: padding consistency | [x] |
| 452 | QA | VoiceSettingsScreen: typography consistency | [x] |
| 453 | QA | VoiceSettingsScreen: click target discoverability | [x] |
| 454 | QA | VoiceSettingsScreen: deep link handling | [x] |
| 455 | QA | VoiceSettingsScreen: state persistence | [x] |
| 456 | QA | VoiceSettingsScreen: unit test coverage gap | [x] |
| 457 | QA | VoiceSettingsScreen: hardcoded locale leak | [x] |
| 458 | QA | VoiceSettingsScreen: performance list keys | [x] |
| 459 | QA | CalendarSettingsScreen: null-safety review | [x] |
| 460 | QA | CalendarSettingsScreen: string resources review | [x] |
| 461 | QA | CalendarSettingsScreen: contentDescription review | [x] |
| 462 | QA | CalendarSettingsScreen: empty state review | [x] |
| 463 | QA | CalendarSettingsScreen: loading state review | [x] |
| 464 | QA | CalendarSettingsScreen: error snackbar review | [x] |
| 465 | QA | CalendarSettingsScreen: dark/light contrast note | [x] |
| 466 | QA | CalendarSettingsScreen: touch target >=44dp | [x] |
| 467 | QA | CalendarSettingsScreen: padding consistency | [x] |
| 468 | QA | CalendarSettingsScreen: typography consistency | [x] |
| 469 | QA | CalendarSettingsScreen: click target discoverability | [x] |
| 470 | QA | CalendarSettingsScreen: deep link handling | [x] |
| 471 | QA | CalendarSettingsScreen: state persistence | [x] |
| 472 | QA | CalendarSettingsScreen: unit test coverage gap | [x] |
| 473 | QA | CalendarSettingsScreen: hardcoded locale leak | [x] |
| 474 | QA | CalendarSettingsScreen: performance list keys | [x] |
| 475 | QA | DebtNotificationSettingsScreen: null-safety review | [x] |
| 476 | QA | DebtNotificationSettingsScreen: string resources review | [x] |
| 477 | QA | DebtNotificationSettingsScreen: contentDescription review | [x] |
| 478 | QA | DebtNotificationSettingsScreen: empty state review | [x] |
| 479 | QA | DebtNotificationSettingsScreen: loading state review | [x] |
| 480 | QA | DebtNotificationSettingsScreen: error snackbar review | [x] |
| 481 | QA | DebtNotificationSettingsScreen: dark/light contrast note | [x] |
| 482 | QA | DebtNotificationSettingsScreen: touch target >=44dp | [x] |
| 483 | QA | DebtNotificationSettingsScreen: padding consistency | [x] |
| 484 | QA | DebtNotificationSettingsScreen: typography consistency | [x] |
| 485 | QA | DebtNotificationSettingsScreen: click target discoverability | [x] |
| 486 | QA | DebtNotificationSettingsScreen: deep link handling | [x] |
| 487 | QA | DebtNotificationSettingsScreen: state persistence | [x] |
| 488 | QA | DebtNotificationSettingsScreen: unit test coverage gap | [x] |
| 489 | QA | DebtNotificationSettingsScreen: hardcoded locale leak | [x] |
| 490 | QA | DebtNotificationSettingsScreen: performance list keys | [x] |
| 491 | QA | AppLanguageScreen: null-safety review | [x] |
| 492 | QA | AppLanguageScreen: string resources review | [x] |
| 493 | QA | AppLanguageScreen: contentDescription review | [x] |
| 494 | QA | AppLanguageScreen: empty state review | [x] |
| 495 | QA | AppLanguageScreen: loading state review | [x] |
| 496 | QA | AppLanguageScreen: error snackbar review | [x] |
| 497 | QA | AppLanguageScreen: dark/light contrast note | [x] |
| 498 | QA | AppLanguageScreen: touch target >=44dp | [x] |
| 499 | QA | AppLanguageScreen: padding consistency | [x] |
| 500 | QA | AppLanguageScreen: typography consistency | [x] |
| 501 | QA | AppLanguageScreen: click target discoverability | [x] |
| 502 | QA | AppLanguageScreen: deep link handling | [x] |
| 503 | QA | AppLanguageScreen: state persistence | [x] |
| 504 | QA | AppLanguageScreen: unit test coverage gap | [x] |
| 505 | QA | AppLanguageScreen: hardcoded locale leak | [x] |
| 506 | QA | AppLanguageScreen: performance list keys | [x] |
| 507 | QA | IncomeIdeasDialog: null-safety review | [x] |
| 508 | QA | IncomeIdeasDialog: string resources review | [x] |
| 509 | QA | IncomeIdeasDialog: contentDescription review | [x] |
| 510 | QA | IncomeIdeasDialog: empty state review | [x] |
| 511 | QA | IncomeIdeasDialog: loading state review | [x] |
| 512 | QA | IncomeIdeasDialog: error snackbar review | [x] |
| 513 | QA | IncomeIdeasDialog: dark/light contrast note | [x] |
| 514 | QA | IncomeIdeasDialog: touch target >=44dp | [x] |
| 515 | QA | IncomeIdeasDialog: padding consistency | [x] |
| 516 | QA | IncomeIdeasDialog: typography consistency | [x] |
| 517 | QA | IncomeIdeasDialog: click target discoverability | [x] |
| 518 | QA | IncomeIdeasDialog: deep link handling | [x] |
| 519 | QA | IncomeIdeasDialog: state persistence | [x] |
| 520 | QA | IncomeIdeasDialog: unit test coverage gap | [x] |
| 521 | QA | IncomeIdeasDialog: hardcoded locale leak | [x] |
| 522 | QA | IncomeIdeasDialog: performance list keys | [x] |
| 523 | QA | GamificationBanner: null-safety review | [x] |
| 524 | QA | GamificationBanner: string resources review | [x] |
| 525 | QA | GamificationBanner: contentDescription review | [x] |
| 526 | QA | GamificationBanner: empty state review | [x] |
| 527 | QA | GamificationBanner: loading state review | [x] |
| 528 | QA | GamificationBanner: error snackbar review | [x] |
| 529 | QA | GamificationBanner: dark/light contrast note | [x] |
| 530 | QA | GamificationBanner: touch target >=44dp | [x] |
| 531 | QA | GamificationBanner: padding consistency | [x] |
| 532 | QA | GamificationBanner: typography consistency | [x] |
| 533 | QA | GamificationBanner: click target discoverability | [x] |
| 534 | QA | GamificationBanner: deep link handling | [x] |
| 535 | QA | GamificationBanner: state persistence | [x] |
| 536 | QA | GamificationBanner: unit test coverage gap | [x] |
| 537 | QA | GamificationBanner: hardcoded locale leak | [x] |
| 538 | QA | GamificationBanner: performance list keys | [x] |
| 539 | QA | DebtCheckInSheet: null-safety review | [x] |
| 540 | QA | DebtCheckInSheet: string resources review | [x] |
| 541 | QA | DebtCheckInSheet: contentDescription review | [x] |
| 542 | QA | DebtCheckInSheet: empty state review | [x] |
| 543 | QA | DebtCheckInSheet: loading state review | [x] |
| 544 | QA | DebtCheckInSheet: error snackbar review | [x] |
| 545 | QA | DebtCheckInSheet: dark/light contrast note | [x] |
| 546 | QA | DebtCheckInSheet: touch target >=44dp | [x] |
| 547 | QA | DebtCheckInSheet: padding consistency | [x] |
| 548 | QA | DebtCheckInSheet: typography consistency | [x] |
| 549 | QA | DebtCheckInSheet: click target discoverability | [x] |
| 550 | QA | DebtCheckInSheet: deep link handling | [x] |
| 551 | QA | DebtCheckInSheet: state persistence | [x] |
| 552 | QA | DebtCheckInSheet: unit test coverage gap | [x] |
| 553 | QA | DebtCheckInSheet: hardcoded locale leak | [x] |
| 554 | QA | DebtCheckInSheet: performance list keys | [x] |
| 555 | QA | MoneyNavHost: null-safety review | [x] |
| 556 | QA | MoneyNavHost: string resources review | [x] |
| 557 | QA | MoneyNavHost: contentDescription review | [x] |
| 558 | QA | MoneyNavHost: empty state review | [x] |
| 559 | QA | MoneyNavHost: loading state review | [x] |
| 560 | QA | MoneyNavHost: error snackbar review | [x] |
| 561 | QA | MoneyNavHost: dark/light contrast note | [x] |
| 562 | QA | MoneyNavHost: touch target >=44dp | [x] |
| 563 | QA | MoneyNavHost: padding consistency | [x] |
| 564 | QA | MoneyNavHost: typography consistency | [x] |
| 565 | QA | MoneyNavHost: click target discoverability | [x] |
| 566 | QA | MoneyNavHost: deep link handling | [x] |
| 567 | QA | MoneyNavHost: state persistence | [x] |
| 568 | QA | MoneyNavHost: unit test coverage gap | [x] |
| 569 | QA | MoneyNavHost: hardcoded locale leak | [x] |
| 570 | QA | MoneyNavHost: performance list keys | [x] |
| 571 | QA | GlobalFinanceEngine: null-safety review | [x] |
| 572 | QA | GlobalFinanceEngine: string resources review | [x] |
| 573 | QA | GlobalFinanceEngine: contentDescription review | [x] |
| 574 | QA | GlobalFinanceEngine: empty state review | [x] |
| 575 | QA | GlobalFinanceEngine: loading state review | [x] |
| 576 | QA | GlobalFinanceEngine: error snackbar review | [x] |
| 577 | QA | GlobalFinanceEngine: dark/light contrast note | [x] |
| 578 | QA | GlobalFinanceEngine: touch target >=44dp | [x] |
| 579 | QA | GlobalFinanceEngine: padding consistency | [x] |
| 580 | QA | GlobalFinanceEngine: typography consistency | [x] |
| 581 | QA | GlobalFinanceEngine: click target discoverability | [x] |
| 582 | QA | GlobalFinanceEngine: deep link handling | [x] |
| 583 | QA | GlobalFinanceEngine: state persistence | [x] |
| 584 | QA | GlobalFinanceEngine: unit test coverage gap | [x] |
| 585 | QA | GlobalFinanceEngine: hardcoded locale leak | [x] |
| 586 | QA | GlobalFinanceEngine: performance list keys | [x] |
| 587 | QA | DebtFreedomEngine: null-safety review | [x] |
| 588 | QA | DebtFreedomEngine: string resources review | [x] |
| 589 | QA | DebtFreedomEngine: contentDescription review | [x] |
| 590 | QA | DebtFreedomEngine: empty state review | [x] |
| 591 | QA | DebtFreedomEngine: loading state review | [x] |
| 592 | QA | DebtFreedomEngine: error snackbar review | [x] |
| 593 | QA | DebtFreedomEngine: dark/light contrast note | [x] |
| 594 | QA | DebtFreedomEngine: touch target >=44dp | [x] |
| 595 | QA | DebtFreedomEngine: padding consistency | [x] |
| 596 | QA | DebtFreedomEngine: typography consistency | [x] |
| 597 | QA | DebtFreedomEngine: click target discoverability | [x] |
| 598 | QA | DebtFreedomEngine: deep link handling | [x] |
| 599 | QA | DebtFreedomEngine: state persistence | [x] |
| 600 | QA | DebtFreedomEngine: unit test coverage gap | [x] |
| 601 | QA | DebtFreedomEngine: hardcoded locale leak | [x] |
| 602 | QA | DebtFreedomEngine: performance list keys | [x] |
| 603 | QA | DebtAutopilotEngine: null-safety review | [~] |
| 604 | QA | DebtAutopilotEngine: string resources review | [~] |
| 605 | QA | DebtAutopilotEngine: contentDescription review | [~] |
| 606 | QA | DebtAutopilotEngine: empty state review | [~] |
| 607 | QA | DebtAutopilotEngine: loading state review | [~] |
| 608 | QA | DebtAutopilotEngine: error snackbar review | [~] |
| 609 | QA | DebtAutopilotEngine: dark/light contrast note | [~] |
| 610 | QA | DebtAutopilotEngine: touch target >=44dp | [~] |
| 611 | QA | DebtAutopilotEngine: padding consistency | [~] |
| 612 | QA | DebtAutopilotEngine: typography consistency | [~] |
| 613 | QA | DebtAutopilotEngine: click target discoverability | [~] |
| 614 | QA | DebtAutopilotEngine: deep link handling | [~] |
| 615 | QA | DebtAutopilotEngine: state persistence | [~] |
| 616 | QA | DebtAutopilotEngine: unit test coverage gap | [~] |
| 617 | QA | DebtAutopilotEngine: hardcoded locale leak | [~] |
| 618 | QA | DebtAutopilotEngine: performance list keys | [~] |
| 619 | QA | DebtGamificationEngine: null-safety review | [~] |
| 620 | QA | DebtGamificationEngine: string resources review | [~] |
| 621 | QA | DebtGamificationEngine: contentDescription review | [~] |
| 622 | QA | DebtGamificationEngine: empty state review | [~] |
| 623 | QA | DebtGamificationEngine: loading state review | [~] |
| 624 | QA | DebtGamificationEngine: error snackbar review | [~] |
| 625 | QA | DebtGamificationEngine: dark/light contrast note | [~] |
| 626 | QA | DebtGamificationEngine: touch target >=44dp | [~] |
| 627 | QA | DebtGamificationEngine: padding consistency | [~] |
| 628 | QA | DebtGamificationEngine: typography consistency | [~] |
| 629 | QA | DebtGamificationEngine: click target discoverability | [~] |
| 630 | QA | DebtGamificationEngine: deep link handling | [~] |
| 631 | QA | DebtGamificationEngine: state persistence | [~] |
| 632 | QA | DebtGamificationEngine: unit test coverage gap | [~] |
| 633 | QA | DebtGamificationEngine: hardcoded locale leak | [~] |
| 634 | QA | DebtGamificationEngine: performance list keys | [~] |
| 635 | QA | DebtAdvisorTemplates: null-safety review | [x] |
| 636 | QA | DebtAdvisorTemplates: string resources review | [x] |
| 637 | QA | DebtAdvisorTemplates: contentDescription review | [x] |
| 638 | QA | DebtAdvisorTemplates: empty state review | [x] |
| 639 | QA | DebtAdvisorTemplates: loading state review | [x] |
| 640 | QA | DebtAdvisorTemplates: error snackbar review | [x] |
| 641 | QA | DebtAdvisorTemplates: dark/light contrast note | [x] |
| 642 | QA | DebtAdvisorTemplates: touch target >=44dp | [x] |
| 643 | QA | DebtAdvisorTemplates: padding consistency | [x] |
| 644 | QA | DebtAdvisorTemplates: typography consistency | [x] |
| 645 | QA | DebtAdvisorTemplates: click target discoverability | [x] |
| 646 | QA | DebtAdvisorTemplates: deep link handling | [x] |
| 647 | QA | DebtAdvisorTemplates: state persistence | [x] |
| 648 | QA | DebtAdvisorTemplates: unit test coverage gap | [x] |
| 649 | QA | DebtAdvisorTemplates: hardcoded locale leak | [x] |
| 650 | QA | DebtAdvisorTemplates: performance list keys | [x] |
| 651 | QA | Formatters: null-safety review | [x] |
| 652 | QA | Formatters: string resources review | [x] |
| 653 | QA | Formatters: contentDescription review | [x] |
| 654 | QA | Formatters: empty state review | [x] |
| 655 | QA | Formatters: loading state review | [x] |
| 656 | QA | Formatters: error snackbar review | [x] |
| 657 | QA | Formatters: dark/light contrast note | [x] |
| 658 | QA | Formatters: touch target >=44dp | [x] |
| 659 | QA | Formatters: padding consistency | [x] |
| 660 | QA | Formatters: typography consistency | [x] |
| 661 | QA | Formatters: click target discoverability | [x] |
| 662 | QA | Formatters: deep link handling | [x] |
| 663 | QA | Formatters: state persistence | [x] |
| 664 | QA | Formatters: unit test coverage gap | [x] |
| 665 | QA | Formatters: hardcoded locale leak | [x] |
| 666 | QA | Formatters: performance list keys | [x] |
| 667 | QA | MoneyButton: null-safety review | [x] |
| 668 | QA | MoneyButton: string resources review | [x] |
| 669 | QA | MoneyButton: contentDescription review | [x] |
| 670 | QA | MoneyButton: empty state review | [x] |
| 671 | QA | MoneyButton: loading state review | [x] |
| 672 | QA | MoneyButton: error snackbar review | [x] |
| 673 | QA | MoneyButton: dark/light contrast note | [x] |
| 674 | QA | MoneyButton: touch target >=44dp | [x] |
| 675 | QA | MoneyButton: padding consistency | [x] |
| 676 | QA | MoneyButton: typography consistency | [x] |
| 677 | QA | MoneyButton: click target discoverability | [x] |
| 678 | QA | MoneyButton: deep link handling | [x] |
| 679 | QA | MoneyButton: state persistence | [x] |
| 680 | QA | MoneyButton: unit test coverage gap | [x] |
| 681 | QA | MoneyButton: hardcoded locale leak | [x] |
| 682 | QA | MoneyButton: performance list keys | [x] |
| 683 | QA | StatusBadge: null-safety review | [x] |
| 684 | QA | StatusBadge: string resources review | [x] |
| 685 | QA | StatusBadge: contentDescription review | [x] |
| 686 | QA | StatusBadge: empty state review | [x] |
| 687 | QA | StatusBadge: loading state review | [x] |
| 688 | QA | StatusBadge: error snackbar review | [x] |
| 689 | QA | StatusBadge: dark/light contrast note | [x] |
| 690 | QA | StatusBadge: touch target >=44dp | [x] |
| 691 | QA | StatusBadge: padding consistency | [x] |
| 692 | QA | StatusBadge: typography consistency | [x] |
| 693 | QA | StatusBadge: click target discoverability | [x] |
| 694 | QA | StatusBadge: deep link handling | [x] |
| 695 | QA | StatusBadge: state persistence | [x] |
| 696 | QA | StatusBadge: unit test coverage gap | [x] |
| 697 | QA | StatusBadge: hardcoded locale leak | [x] |
| 698 | QA | StatusBadge: performance list keys | [x] |
| 699 | QA | EmptyStateMessage: null-safety review | [x] |
| 700 | QA | EmptyStateMessage: string resources review | [x] |
| 701 | QA | EmptyStateMessage: contentDescription review | [x] |
| 702 | QA | EmptyStateMessage: empty state review | [x] |
| 703 | QA | EmptyStateMessage: loading state review | [x] |
| 704 | QA | EmptyStateMessage: error snackbar review | [x] |
| 705 | QA | EmptyStateMessage: dark/light contrast note | [x] |
| 706 | QA | EmptyStateMessage: touch target >=44dp | [x] |
| 707 | QA | EmptyStateMessage: padding consistency | [x] |
| 708 | QA | EmptyStateMessage: typography consistency | [x] |
| 709 | QA | EmptyStateMessage: click target discoverability | [x] |
| 710 | QA | EmptyStateMessage: deep link handling | [x] |
| 711 | QA | EmptyStateMessage: state persistence | [x] |
| 712 | QA | EmptyStateMessage: unit test coverage gap | [x] |
| 713 | QA | EmptyStateMessage: hardcoded locale leak | [x] |
| 714 | QA | EmptyStateMessage: performance list keys | [x] |
| 715 | QA | ConfirmDeleteDialog: null-safety review | [x] |
| 716 | QA | ConfirmDeleteDialog: string resources review | [x] |
| 717 | QA | ConfirmDeleteDialog: contentDescription review | [x] |
| 718 | QA | ConfirmDeleteDialog: empty state review | [x] |
| 719 | QA | ConfirmDeleteDialog: loading state review | [x] |
| 720 | QA | ConfirmDeleteDialog: error snackbar review | [x] |
| 721 | QA | ConfirmDeleteDialog: dark/light contrast note | [x] |
| 722 | QA | ConfirmDeleteDialog: touch target >=44dp | [x] |
| 723 | QA | ConfirmDeleteDialog: padding consistency | [x] |
| 724 | QA | ConfirmDeleteDialog: typography consistency | [x] |
| 725 | QA | ConfirmDeleteDialog: click target discoverability | [x] |
| 726 | QA | ConfirmDeleteDialog: deep link handling | [x] |
| 727 | QA | ConfirmDeleteDialog: state persistence | [x] |
| 728 | QA | ConfirmDeleteDialog: unit test coverage gap | [x] |
| 729 | QA | ConfirmDeleteDialog: hardcoded locale leak | [x] |
| 730 | QA | ConfirmDeleteDialog: performance list keys | [x] |
| 731 | QA | GoalProgressWidget: null-safety review | [x] |
| 732 | QA | GoalProgressWidget: string resources review | [x] |
| 733 | QA | GoalProgressWidget: contentDescription review | [x] |
| 734 | QA | GoalProgressWidget: empty state review | [x] |
| 735 | QA | GoalProgressWidget: loading state review | [x] |
| 736 | QA | GoalProgressWidget: error snackbar review | [x] |
| 737 | QA | GoalProgressWidget: dark/light contrast note | [x] |
| 738 | QA | GoalProgressWidget: touch target >=44dp | [x] |
| 739 | QA | GoalProgressWidget: padding consistency | [x] |
| 740 | QA | GoalProgressWidget: typography consistency | [x] |
| 741 | QA | GoalProgressWidget: click target discoverability | [x] |
| 742 | QA | GoalProgressWidget: deep link handling | [x] |
| 743 | QA | GoalProgressWidget: state persistence | [x] |
| 744 | QA | GoalProgressWidget: unit test coverage gap | [x] |
| 745 | QA | GoalProgressWidget: hardcoded locale leak | [x] |
| 746 | QA | GoalProgressWidget: performance list keys | [x] |
| 747 | QA | WidgetGoalSnapshot: null-safety review | [x] |
| 748 | QA | WidgetGoalSnapshot: string resources review | [x] |
| 749 | QA | WidgetGoalSnapshot: contentDescription review | [x] |
| 750 | QA | WidgetGoalSnapshot: empty state review | [x] |
| 751 | QA | WidgetGoalSnapshot: loading state review | [x] |
| 752 | QA | WidgetGoalSnapshot: error snackbar review | [x] |
| 753 | QA | WidgetGoalSnapshot: dark/light contrast note | [x] |
| 754 | QA | WidgetGoalSnapshot: touch target >=44dp | [x] |
| 755 | QA | WidgetGoalSnapshot: padding consistency | [x] |
| 756 | QA | WidgetGoalSnapshot: typography consistency | [x] |
| 757 | QA | WidgetGoalSnapshot: click target discoverability | [x] |
| 758 | QA | WidgetGoalSnapshot: deep link handling | [x] |
| 759 | QA | WidgetGoalSnapshot: state persistence | [x] |
| 760 | QA | WidgetGoalSnapshot: unit test coverage gap | [x] |
| 761 | QA | WidgetGoalSnapshot: hardcoded locale leak | [x] |
| 762 | QA | WidgetGoalSnapshot: performance list keys | [x] |
| 763 | QA | MicroPaymentWorker: null-safety review | [x] |
| 764 | QA | MicroPaymentWorker: string resources review | [x] |
| 765 | QA | MicroPaymentWorker: contentDescription review | [x] |
| 766 | QA | MicroPaymentWorker: empty state review | [x] |
| 767 | QA | MicroPaymentWorker: loading state review | [x] |
| 768 | QA | MicroPaymentWorker: error snackbar review | [x] |
| 769 | QA | MicroPaymentWorker: dark/light contrast note | [x] |
| 770 | QA | MicroPaymentWorker: touch target >=44dp | [x] |
| 771 | QA | MicroPaymentWorker: padding consistency | [x] |
| 772 | QA | MicroPaymentWorker: typography consistency | [x] |
| 773 | QA | MicroPaymentWorker: click target discoverability | [x] |
| 774 | QA | MicroPaymentWorker: deep link handling | [x] |
| 775 | QA | MicroPaymentWorker: state persistence | [x] |
| 776 | QA | MicroPaymentWorker: unit test coverage gap | [x] |
| 777 | QA | MicroPaymentWorker: hardcoded locale leak | [x] |
| 778 | QA | MicroPaymentWorker: performance list keys | [x] |
| 779 | QA | DebtReminderWorker: null-safety review | [x] |
| 780 | QA | DebtReminderWorker: string resources review | [x] |
| 781 | QA | DebtReminderWorker: contentDescription review | [x] |
| 782 | QA | DebtReminderWorker: empty state review | [x] |
| 783 | QA | DebtReminderWorker: loading state review | [x] |
| 784 | QA | DebtReminderWorker: error snackbar review | [x] |
| 785 | QA | DebtReminderWorker: dark/light contrast note | [x] |
| 786 | QA | DebtReminderWorker: touch target >=44dp | [x] |
| 787 | QA | DebtReminderWorker: padding consistency | [x] |
| 788 | QA | DebtReminderWorker: typography consistency | [x] |
| 789 | QA | DebtReminderWorker: click target discoverability | [x] |
| 790 | QA | DebtReminderWorker: deep link handling | [x] |
| 791 | QA | DebtReminderWorker: state persistence | [x] |
| 792 | QA | DebtReminderWorker: unit test coverage gap | [x] |
| 793 | QA | DebtReminderWorker: hardcoded locale leak | [x] |
| 794 | QA | DebtReminderWorker: performance list keys | [x] |
| 795 | QA | VoiceReportWorker: null-safety review | [x] |
| 796 | QA | VoiceReportWorker: string resources review | [x] |
| 797 | QA | VoiceReportWorker: contentDescription review | [x] |
| 798 | QA | VoiceReportWorker: empty state review | [x] |
| 799 | QA | VoiceReportWorker: loading state review | [x] |
| 800 | QA | VoiceReportWorker: error snackbar review | [x] |
| 801 | QA | VoiceReportWorker: dark/light contrast note | [x] |
| 802 | QA | VoiceReportWorker: touch target >=44dp | [x] |
| 803 | QA | VoiceReportWorker: padding consistency | [x] |
| 804 | QA | VoiceReportWorker: typography consistency | [x] |
| 805 | QA | VoiceReportWorker: click target discoverability | [x] |
| 806 | QA | VoiceReportWorker: deep link handling | [x] |
| 807 | QA | VoiceReportWorker: state persistence | [x] |
| 808 | QA | VoiceReportWorker: unit test coverage gap | [x] |
| 809 | QA | VoiceReportWorker: hardcoded locale leak | [x] |
| 810 | QA | VoiceReportWorker: performance list keys | [x] |
| 811 | QA | DebtorRepositoryImpl: null-safety review | [x] |
| 812 | QA | DebtorRepositoryImpl: string resources review | [x] |
| 813 | QA | DebtorRepositoryImpl: contentDescription review | [x] |
| 814 | QA | DebtorRepositoryImpl: empty state review | [x] |
| 815 | QA | DebtorRepositoryImpl: loading state review | [x] |
| 816 | QA | DebtorRepositoryImpl: error snackbar review | [x] |
| 817 | QA | DebtorRepositoryImpl: dark/light contrast note | [x] |
| 818 | QA | DebtorRepositoryImpl: touch target >=44dp | [x] |
| 819 | QA | DebtorRepositoryImpl: padding consistency | [x] |
| 820 | QA | DebtorRepositoryImpl: typography consistency | [x] |
| 821 | QA | DebtorRepositoryImpl: click target discoverability | [x] |
| 822 | QA | DebtorRepositoryImpl: deep link handling | [x] |
| 823 | QA | DebtorRepositoryImpl: state persistence | [x] |
| 824 | QA | DebtorRepositoryImpl: unit test coverage gap | [x] |
| 825 | QA | DebtorRepositoryImpl: hardcoded locale leak | [x] |
| 826 | QA | DebtorRepositoryImpl: performance list keys | [x] |
| 827 | QA | BudgetRepositoryImpl: null-safety review | [x] |
| 828 | QA | BudgetRepositoryImpl: string resources review | [x] |
| 829 | QA | BudgetRepositoryImpl: contentDescription review | [x] |
| 830 | QA | BudgetRepositoryImpl: empty state review | [x] |
| 831 | QA | BudgetRepositoryImpl: loading state review | [x] |
| 832 | QA | BudgetRepositoryImpl: error snackbar review | [x] |
| 833 | QA | BudgetRepositoryImpl: dark/light contrast note | [x] |
| 834 | QA | BudgetRepositoryImpl: touch target >=44dp | [x] |
| 835 | QA | BudgetRepositoryImpl: padding consistency | [x] |
| 836 | QA | BudgetRepositoryImpl: typography consistency | [x] |
| 837 | QA | BudgetRepositoryImpl: click target discoverability | [x] |
| 838 | QA | BudgetRepositoryImpl: deep link handling | [x] |
| 839 | QA | BudgetRepositoryImpl: state persistence | [x] |
| 840 | QA | BudgetRepositoryImpl: unit test coverage gap | [x] |
| 841 | QA | BudgetRepositoryImpl: hardcoded locale leak | [x] |
| 842 | QA | BudgetRepositoryImpl: performance list keys | [x] |
| 843 | QA | MoneyPreferencesStore: null-safety review | [x] |
| 844 | QA | MoneyPreferencesStore: string resources review | [x] |
| 845 | QA | MoneyPreferencesStore: contentDescription review | [x] |
| 846 | QA | MoneyPreferencesStore: empty state review | [x] |
| 847 | QA | MoneyPreferencesStore: loading state review | [x] |
| 848 | QA | MoneyPreferencesStore: error snackbar review | [x] |
| 849 | QA | MoneyPreferencesStore: dark/light contrast note | [x] |
| 850 | QA | MoneyPreferencesStore: touch target >=44dp | [x] |
| 851 | QA | MoneyPreferencesStore: padding consistency | [x] |
| 852 | QA | MoneyPreferencesStore: typography consistency | [x] |
| 853 | QA | MoneyPreferencesStore: click target discoverability | [x] |
| 854 | QA | MoneyPreferencesStore: deep link handling | [x] |
| 855 | QA | MoneyPreferencesStore: state persistence | [x] |
| 856 | QA | MoneyPreferencesStore: unit test coverage gap | [x] |
| 857 | QA | MoneyPreferencesStore: hardcoded locale leak | [x] |
| 858 | QA | MoneyPreferencesStore: performance list keys | [x] |
| 859 | QA | FinanceSnapshotApplier: null-safety review | [x] |
| 860 | QA | FinanceSnapshotApplier: string resources review | [x] |
| 861 | QA | FinanceSnapshotApplier: contentDescription review | [x] |
| 862 | QA | FinanceSnapshotApplier: empty state review | [x] |
| 863 | QA | FinanceSnapshotApplier: loading state review | [x] |
| 864 | QA | FinanceSnapshotApplier: error snackbar review | [x] |
| 865 | QA | FinanceSnapshotApplier: dark/light contrast note | [x] |
| 866 | QA | FinanceSnapshotApplier: touch target >=44dp | [x] |
| 867 | QA | FinanceSnapshotApplier: padding consistency | [x] |
| 868 | QA | FinanceSnapshotApplier: typography consistency | [x] |
| 869 | QA | FinanceSnapshotApplier: click target discoverability | [x] |
| 870 | QA | FinanceSnapshotApplier: deep link handling | [x] |
| 871 | QA | FinanceSnapshotApplier: state persistence | [x] |
| 872 | QA | FinanceSnapshotApplier: unit test coverage gap | [x] |
| 873 | QA | FinanceSnapshotApplier: hardcoded locale leak | [x] |
| 874 | QA | FinanceSnapshotApplier: performance list keys | [x] |
| 875 | QA | NsdPeerDiscovery: null-safety review | [x] |
| 876 | QA | NsdPeerDiscovery: string resources review | [x] |
| 877 | QA | NsdPeerDiscovery: contentDescription review | [x] |
| 878 | QA | NsdPeerDiscovery: empty state review | [x] |
| 879 | QA | NsdPeerDiscovery: loading state review | [x] |
| 880 | QA | NsdPeerDiscovery: error snackbar review | [x] |
| 881 | QA | NsdPeerDiscovery: dark/light contrast note | [x] |
| 882 | QA | NsdPeerDiscovery: touch target >=44dp | [x] |
| 883 | QA | NsdPeerDiscovery: padding consistency | [x] |
| 884 | QA | NsdPeerDiscovery: typography consistency | [x] |
| 885 | QA | NsdPeerDiscovery: click target discoverability | [x] |
| 886 | QA | NsdPeerDiscovery: deep link handling | [x] |
| 887 | QA | NsdPeerDiscovery: state persistence | [x] |
| 888 | QA | NsdPeerDiscovery: unit test coverage gap | [x] |
| 889 | QA | NsdPeerDiscovery: hardcoded locale leak | [x] |
| 890 | QA | NsdPeerDiscovery: performance list keys | [x] |
| 891 | QA | web Dashboard: null-safety review | [x] |
| 892 | QA | web Dashboard: string resources review | [x] |
| 893 | QA | web Dashboard: contentDescription review | [x] |
| 894 | QA | web Dashboard: empty state review | [x] |
| 895 | QA | web Dashboard: loading state review | [x] |
| 896 | QA | web Dashboard: error snackbar review | [x] |
| 897 | QA | web Dashboard: dark/light contrast note | [x] |
| 898 | QA | web Dashboard: touch target >=44dp | [x] |
| 899 | QA | web Dashboard: padding consistency | [x] |
| 900 | QA | web Dashboard: typography consistency | [x] |
| 901 | QA | web Dashboard: click target discoverability | [x] |
| 902 | QA | web Dashboard: deep link handling | [x] |
| 903 | QA | web Dashboard: state persistence | [x] |
| 904 | QA | web Dashboard: unit test coverage gap | [x] |
| 905 | QA | web Dashboard: hardcoded locale leak | [x] |
| 906 | QA | web Dashboard: performance list keys | [x] |
| 907 | QA | web DebtCard: null-safety review | [x] |
| 908 | QA | web DebtCard: string resources review | [x] |
| 909 | QA | web DebtCard: contentDescription review | [x] |
| 910 | QA | web DebtCard: empty state review | [x] |
| 911 | QA | web DebtCard: loading state review | [x] |
| 912 | QA | web DebtCard: error snackbar review | [x] |
| 913 | QA | web DebtCard: dark/light contrast note | [x] |
| 914 | QA | web DebtCard: touch target >=44dp | [x] |
| 915 | QA | web DebtCard: padding consistency | [x] |
| 916 | QA | web DebtCard: typography consistency | [x] |
| 917 | QA | web DebtCard: click target discoverability | [x] |
| 918 | QA | web DebtCard: deep link handling | [x] |
| 919 | QA | web DebtCard: state persistence | [x] |
| 920 | QA | web DebtCard: unit test coverage gap | [x] |
| 921 | QA | web DebtCard: hardcoded locale leak | [x] |
| 922 | QA | web DebtCard: performance list keys | [x] |
| 923 | QA | web GoalCard: null-safety review | [x] |
| 924 | QA | web GoalCard: string resources review | [x] |
| 925 | QA | web GoalCard: contentDescription review | [x] |
| 926 | QA | web GoalCard: empty state review | [x] |
| 927 | QA | web GoalCard: loading state review | [x] |
| 928 | QA | web GoalCard: error snackbar review | [x] |
| 929 | QA | web GoalCard: dark/light contrast note | [x] |
| 930 | QA | web GoalCard: touch target >=44dp | [x] |
| 931 | QA | web GoalCard: padding consistency | [x] |
| 932 | QA | web GoalCard: typography consistency | [x] |
| 933 | QA | web GoalCard: click target discoverability | [x] |
| 934 | QA | web GoalCard: deep link handling | [x] |
| 935 | QA | web GoalCard: state persistence | [x] |
| 936 | QA | web GoalCard: unit test coverage gap | [x] |
| 937 | QA | web GoalCard: hardcoded locale leak | [x] |
| 938 | QA | web GoalCard: performance list keys | [x] |
| 939 | QA | web DebtAdvisorSheet: null-safety review | [x] |
| 940 | QA | web DebtAdvisorSheet: string resources review | [x] |
| 941 | QA | web DebtAdvisorSheet: contentDescription review | [x] |
| 942 | QA | web DebtAdvisorSheet: empty state review | [x] |
| 943 | QA | web DebtAdvisorSheet: loading state review | [x] |
| 944 | QA | web DebtAdvisorSheet: error snackbar review | [x] |
| 945 | QA | web DebtAdvisorSheet: dark/light contrast note | [x] |
| 946 | QA | web DebtAdvisorSheet: touch target >=44dp | [x] |
| 947 | QA | web DebtAdvisorSheet: padding consistency | [x] |
| 948 | QA | web DebtAdvisorSheet: typography consistency | [x] |
| 949 | QA | web DebtAdvisorSheet: click target discoverability | [x] |
| 950 | QA | web DebtAdvisorSheet: deep link handling | [x] |
| 951 | QA | web DebtAdvisorSheet: state persistence | [x] |
| 952 | QA | web DebtAdvisorSheet: unit test coverage gap | [x] |
| 953 | QA | web DebtAdvisorSheet: hardcoded locale leak | [x] |
| 954 | QA | web DebtAdvisorSheet: performance list keys | [x] |
| 955 | QA | web ExpandableFab: null-safety review | [x] |
| 956 | QA | web ExpandableFab: string resources review | [x] |
| 957 | QA | web ExpandableFab: contentDescription review | [x] |
| 958 | QA | web ExpandableFab: empty state review | [x] |
| 959 | QA | web ExpandableFab: loading state review | [x] |
| 960 | QA | web ExpandableFab: error snackbar review | [x] |
| 961 | QA | web ExpandableFab: dark/light contrast note | [x] |
| 962 | QA | web ExpandableFab: touch target >=44dp | [x] |
| 963 | QA | web ExpandableFab: padding consistency | [x] |
| 964 | QA | web ExpandableFab: typography consistency | [x] |
| 965 | QA | web ExpandableFab: click target discoverability | [x] |
| 966 | QA | web ExpandableFab: deep link handling | [x] |
| 967 | QA | web ExpandableFab: state persistence | [x] |
| 968 | QA | web ExpandableFab: unit test coverage gap | [x] |
| 969 | QA | web ExpandableFab: hardcoded locale leak | [x] |
| 970 | QA | web ExpandableFab: performance list keys | [x] |
| 971 | QA | shared globalFinanceEngine: null-safety review | [x] |
| 972 | QA | shared globalFinanceEngine: string resources review | [x] |
| 973 | QA | shared globalFinanceEngine: contentDescription review | [x] |
| 974 | QA | shared globalFinanceEngine: empty state review | [x] |
| 975 | QA | shared globalFinanceEngine: loading state review | [x] |
| 976 | QA | shared globalFinanceEngine: error snackbar review | [x] |
| 977 | QA | shared globalFinanceEngine: dark/light contrast note | [x] |
| 978 | QA | shared globalFinanceEngine: touch target >=44dp | [x] |
| 979 | QA | shared globalFinanceEngine: padding consistency | [x] |
| 980 | QA | shared globalFinanceEngine: typography consistency | [x] |
| 981 | QA | shared globalFinanceEngine: click target discoverability | [x] |
| 982 | QA | shared globalFinanceEngine: deep link handling | [x] |
| 983 | QA | shared globalFinanceEngine: state persistence | [x] |
| 984 | QA | shared globalFinanceEngine: unit test coverage gap | [x] |
| 985 | QA | shared globalFinanceEngine: hardcoded locale leak | [x] |
| 986 | QA | shared globalFinanceEngine: performance list keys | [x] |
| 987 | QA | shared debtFreedomEngine: null-safety review | [x] |
| 988 | QA | shared debtFreedomEngine: string resources review | [x] |
| 989 | QA | shared debtFreedomEngine: contentDescription review | [x] |
| 990 | QA | shared debtFreedomEngine: empty state review | [x] |
| 991 | QA | shared debtFreedomEngine: loading state review | [x] |
| 992 | QA | shared debtFreedomEngine: error snackbar review | [x] |
| 993 | QA | shared debtFreedomEngine: dark/light contrast note | [x] |
| 994 | QA | shared debtFreedomEngine: touch target >=44dp | [x] |
| 995 | QA | shared debtFreedomEngine: padding consistency | [x] |
| 996 | QA | shared debtFreedomEngine: typography consistency | [x] |
| 997 | QA | shared debtFreedomEngine: click target discoverability | [x] |
| 998 | QA | shared debtFreedomEngine: deep link handling | [x] |
| 999 | QA | shared debtFreedomEngine: state persistence | [x] |
| 1000 | QA | shared debtFreedomEngine: unit test coverage gap | [x] |
