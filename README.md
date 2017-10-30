# Copy as Node.js Request

Burp extension to copy a request as a node.js requests function

Accessed via a Request -> Action -> Copy as Node.js Request

<img src="https://i.imgur.com/Qlo3mto.png">

## Installation
Build the jar using `ant`, then install by navigating to Extender -> Extensions -> Add and select the jar

Please note the output should be saved in it's own folder, as it requires `request` to be in `node_modules`.
 
This can be installed with `npm i request`.
## Sample

The output from the above screenshot would look like: 

```$xslt
var request = require('request');

var burp0_cookie = '_ga=GA1.1.915896612.1504923897; _gid=GA1.1.331984532.1509395051; _gat=1'

var burp0_bodyString = "checked%5B%5D=1&checked%5B%5D=1&checked%5B%5D=1&checked%5B%5D=1&checked%5B%5D=1&checked%5B%5D=1"

var burp0_headers = {
    "Content-Length": "95", 
    "Accept": "*/*", 
    "Origin": "http://localhost:8070", 
    "X-Requested-With": "XMLHttpRequest", 
    "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36", 
    "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8", 
    "DNT": "1", 
    "Referer": "http://localhost:8070/", 
    "Accept-Encoding": "gzip, deflate, br", 
    "Accept-Language": "en-US,en;q=0.8,it;q=0.6", 
    "Connection": "close",
    'Cookie':burp0_cookie
}

var burp0_options = {
    url: "http://localhost:8070/RandomComic/rand",
    headers: burp0_headers,
    method:"post",
    body: dataString
}

function callback(error, response, body) {
    if (!error && response.statusCode == 200) {
        console.log(body);
    }
}

request(options, callback)
```

### Credits

[Original inspiration by PortSwigger's python requests extension](https://github.com/PortSwigger/copy-as-python-requests)