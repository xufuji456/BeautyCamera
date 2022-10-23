
#ifndef MEDIACLOCK_H
#define MEDIACLOCK_H

#include <math.h>

extern "C" {
#include <libavutil/time.h>
}

class MediaClock {

private:
    int m_pause;
    double m_pts;
    double m_speed;
    double m_pts_drift;
    double m_lastUpdate;

public:
    MediaClock();

    virtual ~MediaClock();

    void init();

    void setClock(double pts);

    void setClock(double pts, double time);

    double getClock() const;

    void setSpeed(double speed);

    double getSpeed() const;

    void syncToSlave(MediaClock *slave);

};



#endif //MEDIACLOCK_H
