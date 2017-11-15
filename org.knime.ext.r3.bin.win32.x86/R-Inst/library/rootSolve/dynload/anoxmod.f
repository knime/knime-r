
c initialisation subroutine: initialises the common block with parameter
c values

       subroutine initanox (steadyparms)
       external steadyparms

       double precision parms(9)
       common /myparms/parms

       call steadyparms(9, parms)
       return
       end


c subroutine calculating the rate of change

       subroutine anoxmod (neq, t, y, ydot, yout, ip)
       implicit none
       double precision t, y(*), ydot(*), yout(*)
       double precision OM,O2,SO4,HS
       double precision min, oxicmin, anoxicmin
       integer neq, ip(*)
       double precision D, Flux, r, rox, ks, ks2, BO2, BSO4, BHS
       common /myparms/D, Flux, r, rox, ks, ks2, BO2, BSO4, BHS

         IF (ip(1) < 1) call rexit("nout should be at least 1")

         OM  = y(1)
         O2  = y(2)
         SO4 = y(3)
         HS  = y(4)
         
         Min       = r*OM
         oxicmin   = Min*(O2/(O2+ks))
         anoxicmin = Min*(1-O2/(O2+ks))* SO4/(SO4+ks2)

         ydot(1)  = Flux - oxicmin - anoxicmin
         ydot(2)  = -oxicmin      -2*rox*HS*(O2/(O2+ks)) + D*(BO2-O2)
         ydot(3)  = -0.5*anoxicmin  +rox*HS*(O2/(O2+ks)) + D*(BSO4-SO4)
         ydot(4)  =  0.5*anoxicmin  -rox*HS*(O2/(O2+ks)) + D*(BHS-HS)
  
c         yout(1) = SO4+HS
         
       return
       end


