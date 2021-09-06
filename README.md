### Android Application to Inspect and Modify `ip_no_pmtu_disc` Kernel Parameter

Simply run the application to see the value of `ip_no_pmtu_disc` kernel
parameter. This is equivalent to running onve of the following comamnds:

```sh
$ adb shell "cat /proc/sys/net/ipv4/ip_no_pmtu_disc"
$ adb shell "sysctl -n net.ipv4.ip_no_pmtu_disc"
```

Resetting the value to 0 is a workaround when your device experiences timeouts
while working over WiFi (root access required). To reset, simply tap on 'Reset'
button. This is equivalent to running one of the following commands:

```sh
$ adb shell "su -c 'echo 0 > /proc/sys/net/ipv4/ip_no_pmtu_disc'"
$ adb shell "su -c 'sysctl -w net.ipv4.ip_no_pmtu_disc 0'"
```

**NOTE**: The setting is only active until the device reboot.
`
