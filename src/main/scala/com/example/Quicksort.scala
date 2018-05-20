object Quicksort extends App {
  def quicksort(list: List[Int]):List[Int] = if (list.length <= 1) list else {
    val pivot = list(scala.util.Random.nextInt(list.length))
    quicksort(list.filter(_<pivot)) ++ list.filter(_==pivot) ++ quicksort(list.filter(_>pivot))
  }

  print(quicksort(List(4,2,8,7,21,1,1,0)))
}
