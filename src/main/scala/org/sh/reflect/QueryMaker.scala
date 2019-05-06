
package org.sh.reflect

trait QueryMaker {
  def makeQuery(pid:String, reqName:String, reqData:String):String
  def isConnected:Boolean
}
