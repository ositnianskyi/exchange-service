# Currency exchange service

# How to run

- Register in service https://openexchangerates.org/signup/free and get App Id.
- Set App Id to property `external.openexchangerates.appId` in [properties file](src/main/resources/application.properties)
- Build project:
  ```
  ./gradlew build  
  ```
- Run service:
  ```
  ./gradlew bootRun
  ```

## APIs
- Get current currencies list:
    ```
    curl -X GET http://localhost:8080/currencies
    ```
    Response:
    ```
    ["BTC","USD","CAD"]
    ```

- Add currency to current currencies list:
    ```
    curl -X POST http://localhost:8080/currencies \
    -H "Content-Type: application/json" \
    -d '{"code":"UAH"}'
    ```
    Response:
    ```
    ["BTC","USD","CAD","UAH"]
    ```

- Get currency exchange rates to base currency:
    ```
    curl -X GET http://localhost:8080/exchange-rates?base=UAH
    ```
    Response:
    ```
    {"timestamp":1730916000,"baseCurrency":"UAH","rates":{"BTC":3.23723E-7,"USD":0.024120926696,"CAD":0.033626766819,"UAH":1}}
    ```