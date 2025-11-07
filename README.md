# HttpClient

## Provides the ability to edit and execute HTTP / WebSocket requests from the code editor of IDEA

### features
- Support sending GET, POST and other requests
- Support sending WebSocket request
- Support Environment, build-in variable and build-in method
- Support jump to SpringMVC/Micronaut Controller method from url
- Show SpringMVC/Micronaut Controller method information when hover in url
- Support jump to SpringMVC/Micronaut Controller method bean field from json property
- Show SpringMVC/Micronaut Controller method bean field information when hover in json key
- Support JavaScript previous handler, post handler and global handler(JavaScript syntax highlighting depends on WebCalm 
  plugin. After install WebCalm, please restart IDEA, Otherwise JavaScript syntax highlighting will not work.)
- Support reading file content as http request body
- Support saving http response body to file
- When the response Content-Type is image type, it supports direct preview of the corresponding image
- Support preview HTML response
- Support preview pdf response
- Support searching SpringMVC/Micronaut Api in the SearchEverywhere Dialog
- Support Mock Server

### Getting Started

1. Create a file with an HTTP suffix
2. Type gtr or ptr to trigger live templates and quickly create HTTP requests
3. Click the run button on the left side of the editor to sending request and view the response
