# LocalDHT
A distributed file system which support download and upload file using UDP and only run on local network.

# Implementation
This is a simple distributed file system implementation which is implemented by Kademlia DHT algorithm.
I implement a simplified version of Kademlia algorithm using UDP to build a P2P network and use TCP to transfer files.

# How to run
There is a windows cmd package in the repository. Please modify the settings file and put the new build jar in lib folder.
> Execute run-dht.cmd
> 
```
Select the option:
        0 - Get
        1 - Put
        2 - Search Local
        3 - Download
        4 - Upload
        5 - Show All Entries
        6 - Show All Ips
        7 - Show Local Ip
        99 - Exit
```

# Network
This implementation only support local network only, each peer must in the same local network and shuold not block UDP broadcast.
