# Splashing Helper
A plugin that is designed to help with zero down time when splashing NPCs.

# Additional details
The plugin sets a static, visible (configurable) 20 minute timer when the user starts interacting with an NPC with attack in their right click (context) menu. This timer is reset every time the user interacts with the client (right click, left click, keyboard button press etc). This does not reset on mouse hover, there must be some kind of physical interaction.

It can be configured to alert you when the timer expires, X amount of time **before** the timer expires - and when the NPC you are splashing on dies or despawns.



## Config
| Config name | Description |
| ------------- | ------------- |
| Show timer  | Displays a visual timer infobox on screen to track splashing combat timer (20 minutes). |
| Notify expiration  | Customisable notification to alert you when the splashing timer expires. |
| Notify NPC death | Customisable notification to alert you when the NPC you are splashing on dies or despawns. |
| Notify expiration buffer | Time (seconds) to alert before the splashing combat timer expires. 0 means no notification before timer expiration time. |

## Examples
### Timer info box
![image](https://github.com/user-attachments/assets/3b607b31-b75e-4b38-93e5-b3ce0f859427)

### NPC death
![image](https://github.com/user-attachments/assets/25d1048f-74ad-48fa-9ca2-9a8f46be35ee)

### Notify expiration
![image](https://github.com/user-attachments/assets/7972aa3e-7982-4082-a83e-6776f86b5ed4)

### Notify expiration buffer
![image](https://github.com/user-attachments/assets/9b50c796-8291-4072-9e36-68e14494c6d1)

## Optimal configuration
Set up both the NPC death notification and the expiry notification as such:

![image](https://github.com/user-attachments/assets/c40e8226-d15a-4e49-921a-27654ae9e88f)

If, like me, you're also training a pure, I would recommend configuring the Attack styles plugin to hide the defence attack styles!

![image](https://github.com/user-attachments/assets/61ef1fbc-7bbf-44dd-b3ce-233f4239d6b8)



