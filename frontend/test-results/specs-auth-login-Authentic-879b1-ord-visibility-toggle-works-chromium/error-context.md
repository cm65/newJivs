# Page snapshot

```yaml
- main [ref=e4]:
  - generic [ref=e5]:
    - img [ref=e7]
    - heading "JiVS Platform Login" [level=1] [ref=e9]
    - paragraph [ref=e10]: Enterprise Data Management Platform
    - generic [ref=e11]:
      - generic [ref=e12]:
        - generic:
          - text: Username
          - generic: "*"
        - generic [ref=e13]:
          - textbox "Username" [ref=e14]
          - group:
            - generic: Username *
      - generic [ref=e15]:
        - generic [ref=e16]:
          - text: Password
          - generic [ref=e17]: "*"
        - generic [ref=e18]:
          - textbox "Password" [active] [ref=e19]: test123
          - button [ref=e21] [cursor=pointer]:
            - img [ref=e22]
          - group:
            - generic: Password *
      - generic [ref=e24] [cursor=pointer]:
        - generic [ref=e25]:
          - checkbox "Remember me" [ref=e26]
          - img [ref=e27]
        - generic [ref=e29]: Remember me
      - button "Sign In" [ref=e30] [cursor=pointer]: Sign In
      - generic [ref=e31]:
        - link "Forgot password?" [ref=e33] [cursor=pointer]:
          - /url: "#"
        - link "Don't have an account? Contact Admin" [ref=e35] [cursor=pointer]:
          - /url: "#"
    - paragraph [ref=e36]: © 2025 JiVS Platform. All rights reserved.
```