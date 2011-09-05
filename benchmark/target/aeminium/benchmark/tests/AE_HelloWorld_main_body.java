package aeminium.benchmark.tests;
class AE_HelloWorld_main_body implements aeminium.runtime.Body {
  String[] args;
  AE_HelloWorld_main_body(  String[] args){
    this.args=args;
  }
  public void execute(  aeminium.runtime.Runtime rt,  aeminium.runtime.Task task) throws Exception {
    System.out.println("Hello World!");
  }
}
