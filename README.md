# sqlParser
SQL to Json parser

# Build

```bash
./gradlew  clean build
```
This will build an avaible for usage executable .jar file at build/libs

# Usage

By default, app operates with:
- input.txt;
- output.json

Which are assigned at com.maniaqz.configuration.ParserConfig

Alternatively, you can specify different files while executing through .jar:
```bash
java -jar sqlParser-1.0.0.jar customInputFileName customOutputFileName
```

# Examples

SELECT SQL:
```sql
SELECT
bananas as fruit,
tomato as vegetable
FROM Products
WHERE Price BETWEEN 10 AND 20;
```

SELECT JSON:
```json
{
    "TABLE": ["Products"],
    "CRUD": ["SELECT"],
    "COLUMN": [
        "bananas AS fruit",
        "tomato AS vegetable"
    ],
    "WHERE": ["Price BETWEEN 10 AND 20"]
}
```

=====================================================

JOIN SQL:
```sql
SELECT Customers.CustomerName, Orders.OrderID
FROM Customers
LEFT JOIN Orders
ON Customers.CustomerID=Orders.CustomerID
ORDER BY Customers.CustomerName;
```

JOIN JSON:
```json
{
    "TABLE": ["Customers"],
    "ORDER_BY": ["Customers.CustomerName"],
    "CRUD": ["SELECT"],
    "COLUMN": [
        "Customers.CustomerName",
        "Orders.OrderID"
    ],
    "JOIN 1": ["LEFT JOIN Orders ON Customers.CustomerID = Orders.CustomerID"]
}
```

=====================================================

UNION SQL:
```sql
SELECT City, Country FROM Customers
WHERE Country='Germany'
UNION
SELECT City, Country FROM Suppliers
WHERE Country='Germany'
ORDER BY City;
```

UNION JSON:
```json
{
    "TABLE": ["Customers"],
    "CRUD": ["SELECT"],
    "UNION ANALYSE 1": {
        "TABLE": ["Suppliers"],
        "ORDER_BY": ["City"],
        "CRUD": ["SELECT"],
        "COLUMN": [
            "City",
            "Country"
        ],
        "WHERE": ["Country = 'Germany'"]
    },
    "UNION 1": ["SELECT City, Country FROM Suppliers WHERE Country = 'Germany' ORDER BY City"],
    "COLUMN": [
        "City",
        "Country"
    ],
    "WHERE": ["Country = 'Germany'"]
}
```
