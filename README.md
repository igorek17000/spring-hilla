### 봉데이터 
##### 링크 https://api.bybit.com/v2/public/kline/list?symbol=BTCUSD&interval=5&limit=200&from=1581256500

##### [request]
- symbol 코인명
- interval 시간 간격 설정 파라미터. 1 3 5 15 30 60 120 240 360 720 "D" "M" "W"
- from 시작 시간 설정
- limit 데이터 수 설정. 최대 200개 까지 가능하며 default 값은 200이다.

##### [response]
- symbol 코인명
- period 시간 간격. 5min, 15min, 30min, 1h, 4h, 1d
- open_time 오픈 시간 (시가 기준이라고 생각하면 된다)
- open 시가
- high 최고가
- low	최저가
- close 종가
- volume Trading volume
- turnover Turnover (volume과 turnover 차이 https://bybit-exchange.github.io/docs/inverse/#t-faq_turnover)
