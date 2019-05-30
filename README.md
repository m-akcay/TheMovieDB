# TheMovieDB
Simple app that is able to get popular actors and perform a firstname based search.

How to run -->
    
    Download and extract project * 
    "Open" project using Android Studio
    Build and Run on Android Virtual Device provided by Android Studio **
  
    * Directory must not contain any non-ASCII characters
    ** API level of device must be greater than 21, 
       however, it is only tested on API 23 and API 25.
     
 
 
 Both parts can be tested on UI. 
 
 Part 1 -->
       
       Button "GET PARTIAL LIST" and Button "GET FULL LIST" are used for this part.
       By the design of the API (themoviedb.org), only one page of actors can be queried at a time.
       e.g. multiple pages in one query is not possible
       
       Therefore query should be performed 'N' times with "page=n"
       where 'N' is number of total pages, 'n' is page to be queried 
       to get full list of popular actors. 
       
       Since 'N' is large (800-1000) and it takes time to query that much page,
       alternative "GET PARTIAL LIST" is implemented which only queries 5% of the pages ( N/20 )
       to reduce waiting time on performing different tests.
       
       Progress is shown as "n/N".
       
       Both "GET PARTIAL LIST" and "GET FULL LIST" block themselves and each other
       in other words these functions don't work simultaneously.
       
       Results of queries are combined into a list of 'Actor' objects
       and they are shown on listView. 
       
       API returns results sorted in terms of 'popularity',
       therefore, no need for sorting. 
       
 Part 2 -->
       
       Button "SEARCH" and EditText "nameText" are used for this part.
       
       Search function tries to match input and partial names of 'Actor's in 'Actor' list
       
       underscore (_) represents space (' ') below for readability but space is used in code
       case insensitive
       if input == "", then fetched list is shown
       
       -> input = "Amy" -> modify -> "Amy_" -> length of ("Amy_") = 4
       -> only get 4 characters from 'fullname' of 'Actor' -> "Robert_....." -> "Robe"
                                                           -> "Amyabcd_" -> "Amya"        
                                                           -> "Amy_....." -> "Amy_"
        "Amy_" ?= "Robe"  ----> false
        "Amy_" ?= "Amya"  ----> false
        "Amy_" ?= "Amy_"  ----> true --> add to result list
       
 Deficiencies and What to do next --->
      
      If "SEARCH" function will be used more frequently than "GET LIST" functions (most probably),
      it will be appropriate to keep another list of 'Actor's that is sorted by name
      to allow binarySearch for improving performance.
    
      
      ListView "resultListView" can be modified
      so that if one element ('Actor') is clicked, his/her photo will pop up.
      
      'Actor's can be given 'rank's according to their popularity (1 = most popular),
      such that, after search operation, user can see the 'rank's of the resulting 'Actor's.
      --> searched = Amy
      --> results = 4->Amy X  ....., 49->Amy Y ......, 461->Amy Z ....... 
      
      API has a limit of queries --> {"status_code":25,"status_message":"Your request count (44) is over the allowed limit of 40."}
      Therefore query for each page can be performed up to REQUEST_LIMIT which is set to 10,
      however, maximum successive fails of the same query is seen to be 5 over 20 trials.
      Some delay time may be added after 40th query to solve this problem.
      
      
      
