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

### Jar
##### [Socket Jar 역활]
- 봉별 거래가 되고 있는지 체크
- 거래가 되고 있다면 ?
- 거래가 안되고 있다면 봉기준 데이터를 이용하여 지점에 도달했는지 계산
  1. [거래 금액이 달라졌을시만 체크]
  2. [common_trace_set] 데이터가 성공적으로 값이 있을시만 이전 금액 저장해야함
  3. 실행시 최초 한번만 조회
- 지점에 도달했을시 이쪽으로 Rest Api 실행

##### [Socket Jar 주의점]
1. 중복 실행 안되게 설정
2. common_trace_set 종료안된 시점에서 common_trace_start 실행하면 안됨
3. 항상 실행되고 있음 individual_check